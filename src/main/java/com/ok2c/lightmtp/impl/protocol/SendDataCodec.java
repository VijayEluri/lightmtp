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
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.util.LinkedList;

import com.ok2c.lightmtp.SMTPCodes;
import com.ok2c.lightmtp.SMTPConsts;
import com.ok2c.lightmtp.SMTPProtocolException;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.message.SMTPContent;
import com.ok2c.lightmtp.message.SMTPMessageParser;
import com.ok2c.lightmtp.message.SMTPReplyParser;
import com.ok2c.lightmtp.protocol.DeliveryRequest;
import com.ok2c.lightmtp.protocol.ProtocolCodec;
import com.ok2c.lightmtp.protocol.ProtocolCodecs;
import com.ok2c.lightmtp.protocol.RcptResult;
import com.ok2c.lightnio.IOSession;
import com.ok2c.lightnio.SessionInputBuffer;
import com.ok2c.lightnio.SessionOutputBuffer;
import com.ok2c.lightnio.buffer.CharArrayBuffer;

public class SendDataCodec implements ProtocolCodec<ClientState> {

    private final static int BUF_SIZE = 8 * 1024;
    private final static int LINE_SIZE = 1 * 1024;
    private final static int LIMIT = BUF_SIZE - LINE_SIZE;
    private final static ByteBuffer PERIOD = ByteBuffer.wrap(new byte[] { '.'} );

    enum CodecState {

        CONTENT_READY,
        CONTENT_RESPONSE_EXPECTED,
        COMPLETED

    }

    private final SMTPBuffers iobuffers;
    private final int maxLineLen;
    private final DataAckMode mode;
    private final SMTPMessageParser<SMTPReply> parser;
    private final SMTPInputBuffer contentBuf;
    private final CharArrayBuffer lineBuf;
    private final LinkedList<String> recipients;

    private SMTPContent<ReadableByteChannel> content;
    private ReadableByteChannel contentChannel;
    private boolean contentSent;
    private CodecState codecState;

    public SendDataCodec(
            final SMTPBuffers iobuffers, 
            int maxLineLen, boolean enhancedCodes, final DataAckMode mode) {
        super();
        if (iobuffers == null) {
            throw new IllegalArgumentException("IO buffers may not be null");
        }
        this.iobuffers = iobuffers;
        this.maxLineLen = maxLineLen;
        this.mode = mode != null ? mode : DataAckMode.SINGLE;
        this.parser = new SMTPReplyParser(enhancedCodes);
        this.contentBuf = new SMTPInputBuffer(BUF_SIZE, LINE_SIZE);
        this.lineBuf = new CharArrayBuffer(LINE_SIZE);
        this.recipients = new LinkedList<String>();
        this.codecState = CodecState.CONTENT_READY;
    }

    public SendDataCodec(final SMTPBuffers iobuffers, 
            boolean enhancedCodes, final DataAckMode mode) {
        this(iobuffers, SMTPConsts.MAX_LINE_LEN, enhancedCodes, mode);
    }

    public SendDataCodec(final SMTPBuffers iobuffers, boolean enhancedCodes) {
        this(iobuffers, SMTPConsts.MAX_LINE_LEN, enhancedCodes, DataAckMode.SINGLE);
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
        if (sessionState.getRequest() == null) {
            throw new IllegalArgumentException("Delivery request may not be null");
        }
        
        DeliveryRequest request = sessionState.getRequest();        
        
        this.parser.reset();
        this.contentBuf.clear();
        this.lineBuf.clear();
        this.recipients.clear();
        if (this.mode.equals(DataAckMode.PER_RECIPIENT)) {
            this.recipients.addAll(request.getRecipients());
        }
        
        this.content = request.getContent();
        this.contentChannel = this.content.channel();
        this.contentSent = false;
        this.codecState = CodecState.CONTENT_READY;

        iosession.setEvent(SelectionKey.OP_WRITE);
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

        SessionOutputBuffer buf = this.iobuffers.getOutbuf();

        switch (this.codecState) {
        case CONTENT_READY:
            while (buf.length() < LIMIT) {
                int bytesRead = 0;
                if (!this.contentBuf.hasData()) {
                    bytesRead = this.contentBuf.fill(this.contentChannel);
                }

                boolean lineComplete = this.contentBuf.readLine(this.lineBuf, bytesRead == -1);
                if (this.maxLineLen > 0 &&
                        (this.lineBuf.length() > this.maxLineLen ||
                                (!lineComplete && this.contentBuf.length() > this.maxLineLen))) {
                    throw new SMTPProtocolException("Maximum line length limit exceeded");
                }
                if (lineComplete) {
                    if (this.lineBuf.length() > 0 && this.lineBuf.charAt(0) == '.') {
                        buf.write(PERIOD);
                    }
                    buf.writeLine(this.lineBuf);
                    this.lineBuf.clear();
                } else {
                    bytesRead = this.contentBuf.fill(this.contentChannel);
                }
                if (bytesRead == -1 && !this.contentBuf.hasData()) {

                    this.lineBuf.clear();
                    this.lineBuf.append('.');
                    buf.writeLine(this.lineBuf);
                    this.lineBuf.clear();

                    this.content.reset();
                    this.contentSent = true;
                    this.codecState = CodecState.CONTENT_RESPONSE_EXPECTED;
                    break;
                }
                if (bytesRead == 0 && !lineComplete) {
                    break;
                }
            }
        }

        if (buf.hasData()) {
            buf.flush(iosession.channel());
        }
        if (!buf.hasData() && this.contentSent) {
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
            case CONTENT_RESPONSE_EXPECTED:
                if (this.mode.equals(DataAckMode.PER_RECIPIENT)) {
                    String recipient = this.recipients.removeFirst();
                    if (reply.getCode() != SMTPCodes.OK) {
                        sessionState.getFailures().add(new RcptResult(reply, recipient));
                    }
                }
                if (this.recipients.isEmpty()) {
                    this.codecState = CodecState.COMPLETED;
                }
                sessionState.setReply(reply);
                break;
            default:
                throw new SMTPProtocolException("Unexpected reply: " + reply);
            }
        }
    }

    public boolean isIdle() {
        return this.codecState == CodecState.CONTENT_READY;
    }

    public boolean isCompleted() {
        return this.codecState == CodecState.COMPLETED;
    }

    public String next(
            final ProtocolCodecs<ClientState> codecs,
            final ClientState sessionState) {
        if (isCompleted()) {
            if (sessionState.isTerminated()) {
                return ProtocolState.QUIT.name();
            }
            return ProtocolState.MAIL.name();
        } else {
            return null;
        }
    }

}
