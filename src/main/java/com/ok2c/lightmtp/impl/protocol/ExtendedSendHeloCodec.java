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
import com.ok2c.lightmtp.util.DNSUtils;
import com.ok2c.lightnio.IOSession;
import com.ok2c.lightnio.SessionInputBuffer;
import com.ok2c.lightnio.SessionOutputBuffer;

public class ExtendedSendHeloCodec implements ProtocolCodec<ClientSessionState> {

    enum CodecState {

        SERVICE_READY_EXPECTED,
        EHLO_READY,
        EHLO_RESPONSE_EXPECTED,
        HELO_READY,
        HELO_RESPONSE_EXPECTED,
        COMPLETED

    }

    private final SMTPMessageParser<SMTPReply> parser;
    private final SMTPMessageWriter<SMTPCommand> writer;

    private CodecState codecState;

    public ExtendedSendHeloCodec() {
        super();
        this.parser = new SMTPReplyParser();
        this.writer = new SMTPCommandWriter();
        this.codecState = CodecState.SERVICE_READY_EXPECTED;
    }

    public void reset(
            final IOSession iosession,
            final ClientSessionState sessionState) throws IOException, SMTPProtocolException {
        this.parser.reset();
        this.writer.reset();
        this.codecState = CodecState.SERVICE_READY_EXPECTED;

        iosession.setEventMask(SelectionKey.OP_READ);
    }

    public void produceData(
            final IOSession iosession,
            final ClientSessionState sessionState) throws IOException, SMTPProtocolException {
        if (iosession == null) {
            throw new IllegalArgumentException("IO session may not be null");
        }
        if (sessionState == null) {
            throw new IllegalArgumentException("Session state may not be null");
        }

        SessionOutputBuffer buf = sessionState.getOutbuf();

        switch (this.codecState) {
        case EHLO_READY:
            SMTPCommand ehlo = new SMTPCommand("EHLO",
                    DNSUtils.getLocalDomain(iosession.getLocalAddress()));
            this.writer.write(ehlo, buf);
            this.codecState = CodecState.EHLO_RESPONSE_EXPECTED;
            break;
        case HELO_READY:
            SMTPCommand helo = new SMTPCommand("HELO",
                    DNSUtils.getLocalDomain(iosession.getLocalAddress()));
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

    public void consumeData(
            final IOSession iosession,
            final ClientSessionState sessionState) throws IOException, SMTPProtocolException {
        if (iosession == null) {
            throw new IllegalArgumentException("IO session may not be null");
        }
        if (sessionState == null) {
            throw new IllegalArgumentException("Session state may not be null");
        }

        SessionInputBuffer buf = sessionState.getInbuf();

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
                } else if (reply.getCode() == SMTPCodes.SYNTAX_ERR_COMMAND) {
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
                throw new SMTPProtocolException("Unexpected reply: " + reply);
            }
        }

        if (bytesRead == -1) {
            throw new UnexpectedEndOfStreamException();
        }
    }

    public boolean isIdle() {
        return this.codecState == CodecState.SERVICE_READY_EXPECTED;
    }

    public boolean isCompleted() {
        return this.codecState == CodecState.COMPLETED;
    }

    public String next(
            final ProtocolCodecs<ClientSessionState> codecs,
            final ClientSessionState sessionState) {
        if (isCompleted()) {

            Set<String> exts = sessionState.getExtensions();

            boolean pipelining = exts.contains(SMTPExtensions.PIPELINING);
            boolean enhancedCodes = exts.contains(SMTPExtensions.ENHANCEDSTATUSCODES);

            if (pipelining) {
                codecs.register(ProtocolState.MAIL.name(),
                        new PipeliningSendMailTrxCodec(enhancedCodes));
                codecs.register(ProtocolState.DATA.name(),
                        new SendDataCodec(enhancedCodes));
            }

            return ProtocolState.MAIL.name();
        } else {
            return null;
        }
    }

}
