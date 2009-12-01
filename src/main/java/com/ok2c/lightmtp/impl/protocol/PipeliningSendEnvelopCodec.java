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
import java.util.LinkedList;

import com.ok2c.lightmtp.SMTPCodes;
import com.ok2c.lightmtp.SMTPCommand;
import com.ok2c.lightmtp.SMTPConsts;
import com.ok2c.lightmtp.SMTPProtocolException;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.message.SMTPCommandWriter;
import com.ok2c.lightmtp.message.SMTPMessageParser;
import com.ok2c.lightmtp.message.SMTPMessageWriter;
import com.ok2c.lightmtp.message.SMTPReplyParser;
import com.ok2c.lightmtp.protocol.ProtocolCodec;
import com.ok2c.lightmtp.protocol.ProtocolCodecs;
import com.ok2c.lightmtp.protocol.RcptResult;
import com.ok2c.lightmtp.protocol.DeliveryRequest;
import com.ok2c.lightnio.IOSession;
import com.ok2c.lightnio.SessionInputBuffer;
import com.ok2c.lightnio.SessionOutputBuffer;

public class PipeliningSendEnvelopCodec implements ProtocolCodec<ClientState> {

    enum CodecState {

        MAIL_REQUEST_READY,
        MAIL_RESPONSE_EXPECTED,
        RCPT_RESPONSE_EXPECTED,
        DATA_RESPONSE_EXPECTED,
        COMPLETED

    }

    private final SMTPBuffers iobuffers;
    private final SMTPMessageParser<SMTPReply> parser;
    private final SMTPMessageWriter<SMTPCommand> writer;
    private final LinkedList<String> recipients;

    private CodecState codecState;
    private boolean deliveryFailed;

    public PipeliningSendEnvelopCodec(final SMTPBuffers iobuffers, boolean enhancedCodes) {
        super();
        if (iobuffers == null) {
            throw new IllegalArgumentException("IO buffers may not be null");
        }
        this.iobuffers = iobuffers;
        this.parser = new SMTPReplyParser(enhancedCodes);
        this.writer = new SMTPCommandWriter();
        this.recipients = new LinkedList<String>();
        this.codecState = CodecState.MAIL_REQUEST_READY;
        this.deliveryFailed = false;
    }

    public void cleanUp() {
    }

    public void reset(
            final IOSession iosession,
            final ClientState sessionState) throws IOException, SMTPProtocolException {
        if (iosession == null) {
            throw new IllegalArgumentException("IO session may not be null");
        }
        if (sessionState == null) {
            throw new IllegalArgumentException("Session state may not be null");
        }
        this.writer.reset();
        this.parser.reset();
        this.recipients.clear();
        this.iobuffers.setInputCharset(SMTPConsts.ASCII);
        this.codecState = CodecState.MAIL_REQUEST_READY;
        this.deliveryFailed = false;

        if (sessionState.getRequest() != null) {
            iosession.setEvent(SelectionKey.OP_WRITE);
        } else {
            iosession.setEvent(SelectionKey.OP_READ);
        }
    }

    public void produceData(
            final IOSession iosession,
            final ClientState sessionState) throws IOException, SMTPProtocolException {
        if (iosession == null) {
            throw new IllegalArgumentException("IO session may not be null");
        }
        if (sessionState == null) {
            throw new IllegalArgumentException("Session state may not be null");
        }
        if (sessionState.getRequest() == null) {
            if (sessionState.isTerminated()) {
                this.codecState = CodecState.COMPLETED;
            }
            return;
        }
        
        SessionOutputBuffer buf = this.iobuffers.getOutbuf();
        DeliveryRequest request = sessionState.getRequest();

        switch (this.codecState) {
        case MAIL_REQUEST_READY:
            SMTPCommand mailFrom = new SMTPCommand("MAIL",
                    "FROM:<" + request.getSender() + ">");
            this.writer.write(mailFrom, buf);

            this.recipients.addAll(request.getRecipients());

            for (String recipient: request.getRecipients()) {
                SMTPCommand rcptTo = new SMTPCommand("RCPT", "TO:<" + recipient + ">");
                this.writer.write(rcptTo, buf);
            }
            SMTPCommand data = new SMTPCommand("DATA");
            this.writer.write(data, buf);
            this.codecState = CodecState.MAIL_RESPONSE_EXPECTED;
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
            final ClientState sessionState) throws IOException, SMTPProtocolException {
        if (iosession == null) {
            throw new IllegalArgumentException("IO session may not be null");
        }
        if (sessionState == null) {
            throw new IllegalArgumentException("Session state may not be null");
        }

        SessionInputBuffer buf = this.iobuffers.getInbuf();

        while (this.codecState != CodecState.COMPLETED) {
            int bytesRead = buf.fill(iosession.channel());
            SMTPReply reply = this.parser.parse(buf, bytesRead == -1);

            if (reply == null) {
                if (bytesRead == -1 && !sessionState.isTerminated()) {
                    throw new UnexpectedEndOfStreamException();
                } else {
                    break;
                }
            }

            switch (this.codecState) {
            case MAIL_RESPONSE_EXPECTED:
                this.codecState = CodecState.RCPT_RESPONSE_EXPECTED;
                if (reply.getCode() != SMTPCodes.OK) {
                    this.deliveryFailed = true;
                    sessionState.setReply(reply);
                }
                break;
            case RCPT_RESPONSE_EXPECTED:
                String recipient = this.recipients.removeFirst();

                if (reply.getCode() != SMTPCodes.OK) {
                    sessionState.getFailures().add(new RcptResult(reply, recipient));
                }
                if (this.recipients.isEmpty()) {
                    this.codecState = CodecState.DATA_RESPONSE_EXPECTED;
                }
                break;
            case DATA_RESPONSE_EXPECTED:
                this.codecState = CodecState.COMPLETED;
                if (reply.getCode() != SMTPCodes.START_MAIL_INPUT) {
                    this.deliveryFailed = true;
                }
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
        }
    }

    public boolean isCompleted() {
        return this.codecState == CodecState.COMPLETED;
    }

    public String next(
            final ProtocolCodecs<ClientState> codecs,
            final ClientState sessionState) {
        if (this.codecState == CodecState.COMPLETED) {
            if (sessionState.isTerminated()) {
                return ProtocolState.QUIT.name();
            }
            if (this.deliveryFailed) {
                return ProtocolState.RSET.name();
            } else {
                return ProtocolState.DATA.name();
            }
        } else {
            return null;
        }
    }

}
