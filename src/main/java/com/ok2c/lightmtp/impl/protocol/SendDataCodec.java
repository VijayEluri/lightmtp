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
import java.nio.charset.Charset;

import com.ok2c.lightmtp.SMTPConsts;
import com.ok2c.lightmtp.SMTPProtocolException;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.message.SMTPContent;
import com.ok2c.lightmtp.message.SMTPMessageParser;
import com.ok2c.lightmtp.message.SMTPReplyParser;
import com.ok2c.lightmtp.protocol.ProtocolCodec;
import com.ok2c.lightmtp.protocol.ProtocolCodecs;
import com.ok2c.lightnio.IOSession;
import com.ok2c.lightnio.SessionInputBuffer;
import com.ok2c.lightnio.SessionOutputBuffer;
import com.ok2c.lightnio.buffer.CharArrayBuffer;
import com.ok2c.lightnio.impl.SessionInputBufferImpl;

public class SendDataCodec implements ProtocolCodec<SessionState> {
    
    private final static int BUF_SIZE = 8 * 1024;
    private final static int LINE_SIZE = 1 * 1024;
    private final static int LIMIT = BUF_SIZE - LINE_SIZE;
    private final static ByteBuffer PERIOD = ByteBuffer.wrap(new byte[] { '.'} ); 
    
    private static class ContentBuffer extends SessionInputBufferImpl {
        
        ContentBuffer(final Charset charset) {
            super(BUF_SIZE, LINE_SIZE, charset);
        }
        
        public void clear() {
            super.clear();
        }
        
    }
    
    enum CodecState {
        
        CONTENT_READY,
        CONTENT_RESPONSE_EXPECTED,
        COMPLETED
        
    }
    
    private final int maxLineLen;
    private final SMTPMessageParser<SMTPReply> parser;
    private final ContentBuffer contentBuf;
    private final CharArrayBuffer lineBuf;
    
    private SMTPContent<ReadableByteChannel> content;
    private ReadableByteChannel contentChannel;
    private boolean contentSent;
    private CodecState codecState;
    
    public SendDataCodec(int maxLineLen, boolean enhancedCodes) {
        super();
        this.maxLineLen = maxLineLen;
        this.parser = new SMTPReplyParser(enhancedCodes);
        this.contentBuf = new ContentBuffer(SMTPConsts.ASCII);
        this.lineBuf = new CharArrayBuffer(LINE_SIZE);
        this.codecState = CodecState.CONTENT_READY; 
    }

    public SendDataCodec(boolean enhancedCodes) {
        this(SMTPConsts.MAX_LINE_LEN, enhancedCodes);
    }

    public void reset(
            final IOSession iosession, 
            final SessionState sessionState) throws IOException, SMTPProtocolException {
        if (iosession == null) {
            throw new IllegalArgumentException("IO session may not be null");
        }
        if (sessionState == null) {
            throw new IllegalArgumentException("Session state may not be null");
        }
        if (sessionState.getRequest() == null) {
            throw new IllegalArgumentException("Delivery request may not be null");
        }
        this.parser.reset();
        this.contentBuf.clear();
        this.lineBuf.clear();
        this.content = sessionState.getRequest().getContent();
        this.contentChannel = this.content.channel();
        this.contentSent = false;
        this.codecState = CodecState.CONTENT_READY;
        
        iosession.setEvent(SelectionKey.OP_WRITE);
    }

    public void produceData(
            final IOSession iosession, 
            final SessionState sessionState) throws IOException, SMTPProtocolException {
        if (iosession == null) {
            throw new IllegalArgumentException("IO session may not be null");
        }
        if (sessionState == null) {
            throw new IllegalArgumentException("Session state may not be null");
        }

        SessionOutputBuffer buf = sessionState.getOutbuf();

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
                                (!lineComplete && buf.length() > this.maxLineLen))) {
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
                    buf.writeLine(this.lineBuf);
                    this.lineBuf.append('.');
                    buf.writeLine(this.lineBuf);
                    this.lineBuf.clear();
                    
                    this.content.finish();
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
            final SessionState sessionState) throws IOException, SMTPProtocolException {
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
            case CONTENT_RESPONSE_EXPECTED:
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
        return this.codecState == CodecState.CONTENT_READY; 
    }

    public boolean isCompleted() {
        return this.codecState == CodecState.COMPLETED; 
    }

    public String next(
            final ProtocolCodecs<SessionState> codecs, 
            final SessionState sessionState) {
        if (isCompleted()) {
            return ProtocolState.MAIL.name();
        } else {
            return null;
        }
    }
        
}
