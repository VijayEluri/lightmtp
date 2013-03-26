/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.ok2c.lightmtp.impl.protocol;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.SessionInputBuffer;
import org.apache.http.nio.reactor.SessionOutputBuffer;
import org.apache.http.util.Args;

import com.ok2c.lightmtp.SMTPCodes;
import com.ok2c.lightmtp.SMTPCommand;
import com.ok2c.lightmtp.SMTPExtensions;
import com.ok2c.lightmtp.SMTPProtocolException;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.message.SMTPCommandWriter;
import com.ok2c.lightmtp.message.SMTPMessageParser;
import com.ok2c.lightmtp.message.SMTPMessageWriter;
import com.ok2c.lightmtp.message.SMTPReplyParser;
import com.ok2c.lightmtp.protocol.ProtocolCodec;
import com.ok2c.lightmtp.protocol.ProtocolCodecs;
import com.ok2c.lightmtp.util.AddressUtils;

public class ExtendedSendHeloCodec implements ProtocolCodec<ClientState> {

    enum CodecState {

        SERVICE_READY_EXPECTED,
        EHLO_READY,
        EHLO_RESPONSE_EXPECTED,
        HELO_READY,
        HELO_RESPONSE_EXPECTED,
        COMPLETED

    }

    private final SMTPBuffers iobuffers;
    private final SMTPMessageParser<SMTPReply> parser;
    private final SMTPMessageWriter<SMTPCommand> writer;

    private CodecState codecState;

	private final String heloName;
    private final boolean useAuth;

    public ExtendedSendHeloCodec(final SMTPBuffers iobuffers, final String heloName, final boolean useAuth) {
        super();
        Args.notNull(iobuffers, "IO buffers");
        this.iobuffers = iobuffers;
        this.parser = new SMTPReplyParser();
        this.writer = new SMTPCommandWriter();
        this.codecState = CodecState.SERVICE_READY_EXPECTED;
        this.heloName = heloName;
        this.useAuth = useAuth;
    }


    public ExtendedSendHeloCodec(final SMTPBuffers iobuffers) {
        this(iobuffers, null, false);
    }

    @Override
    public void reset(
            final IOSession iosession,
            final ClientState sessionState) throws IOException, SMTPProtocolException {
        this.parser.reset();
        this.writer.reset();
        this.codecState = CodecState.SERVICE_READY_EXPECTED;

        iosession.setEventMask(SelectionKey.OP_READ);
    }

    @Override
    public void cleanUp() {
    }

    @Override
    public void produceData(
            final IOSession iosession,
            final ClientState sessionState) throws IOException, SMTPProtocolException {
        Args.notNull(iosession, "IO session");
        Args.notNull(sessionState, "Session state");

        SessionOutputBuffer buf = this.iobuffers.getOutbuf();

        String myHelo = heloName;
        if (myHelo == null) {
            myHelo = AddressUtils.getLocalDomain(iosession.getLocalAddress());
        }
        switch (this.codecState) {
        case EHLO_READY:
            SMTPCommand ehlo = new SMTPCommand("EHLO", myHelo);
            this.writer.write(ehlo, buf);
            this.codecState = CodecState.EHLO_RESPONSE_EXPECTED;
            break;
        case HELO_READY:
            SMTPCommand helo = new SMTPCommand("HELO", myHelo);
            this.writer.write(helo, buf);
            this.codecState = CodecState.HELO_RESPONSE_EXPECTED;
            break;
        }

        if (buf.hasData()) {
            buf.flush(iosession.channel());
        }
        if (!buf.hasData()) {
            iosession.clearEvent(SelectionKey.OP_WRITE);
        }
    }

    @Override
    public void consumeData(
            final IOSession iosession,
            final ClientState sessionState) throws IOException, SMTPProtocolException {
        Args.notNull(iosession, "IO session");
        Args.notNull(sessionState, "Session state");

        SessionInputBuffer buf = this.iobuffers.getInbuf();

        int bytesRead = buf.fill(iosession.channel());
        SMTPReply reply = this.parser.parse(buf, bytesRead == -1);

        if (reply != null) {
            switch (this.codecState) {
            case SERVICE_READY_EXPECTED:
                if (reply.getCode() == SMTPCodes.SERVICE_READY) {
                    this.codecState = CodecState.EHLO_READY;
                    iosession.setEvent(SelectionKey.OP_WRITE);
                } else {
                    this.codecState = CodecState.COMPLETED;
                    sessionState.setReply(reply);
                }
                break;
            case EHLO_RESPONSE_EXPECTED:
                if (reply.getCode() == SMTPCodes.OK) {

                    Set<String> extensions = sessionState.getExtensions();

                    List<String> lines = reply.getLines();
                    if (lines.size() > 1) {
                        for (int i = 1; i < lines.size(); i++) {
                            String line = lines.get(i);
                            extensions.add(line.toUpperCase(Locale.US));
                        }
                    }
                    this.codecState = CodecState.COMPLETED;
                    sessionState.setReply(reply);
                } else if (reply.getCode() == SMTPCodes.ERR_PERM_SYNTAX_ERR_COMMAND) {
                    this.codecState = CodecState.HELO_READY;
                    iosession.setEvent(SelectionKey.OP_WRITE);
                } else {
                    this.codecState = CodecState.COMPLETED;
                    sessionState.setReply(reply);
                }
                break;
            case HELO_RESPONSE_EXPECTED:
                this.codecState = CodecState.COMPLETED;
                sessionState.setReply(reply);
                break;
            default:
                if (reply.getCode() == SMTPCodes.ERR_TRANS_SERVICE_NOT_AVAILABLE) {
                    sessionState.setReply(reply);
                    this.codecState = CodecState.COMPLETED;
                } else {
                    throw new SMTPProtocolException("Unexpected reply: " + reply);
                }
            }
        } else {
            if (bytesRead == -1 && !sessionState.isTerminated()) {
                throw new UnexpectedEndOfStreamException();
            }
        }
    }

    @Override
    public boolean isCompleted() {
        return this.codecState == CodecState.COMPLETED;
    }

    @Override
    public String next(
            final ProtocolCodecs<ClientState> codecs,
            final ClientState sessionState) {
        if (isCompleted()) {

            Set<String> exts = sessionState.getExtensions();

            boolean pipelining = exts.contains(SMTPExtensions.PIPELINING);
            boolean enhancedCodes = exts.contains(SMTPExtensions.ENHANCEDSTATUSCODES);

            if (pipelining) {
                codecs.register(ProtocolState.MAIL.name(),
                        new PipeliningSendEnvelopCodec(this.iobuffers, enhancedCodes));
                codecs.register(ProtocolState.DATA.name(),
                        new SendDataCodec(this.iobuffers, enhancedCodes));
            }
            if (useAuth) {
                return ProtocolState.AUTH.name();
            } else {
                return ProtocolState.MAIL.name();
            }
        } else {
            return null;
        }
    }

}
