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
package com.ok2c.lightmtp.protocol.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.charset.Charset;

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
    private final static Charset ASCII = Charset.forName("ASCII");
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
    
    private final SMTPMessageParser<SMTPReply> parser;
    private final ContentBuffer contentBuf;
    private final CharArrayBuffer lineBuf;
    
    private SMTPContent<ReadableByteChannel> content;
    private ReadableByteChannel contentChannel;
    private CodecState codecState;
    
    public SendDataCodec(boolean enhancedCodes) {
        super();
        this.parser = new SMTPReplyParser(enhancedCodes);
        this.contentBuf = new ContentBuffer(ASCII);
        this.lineBuf = new CharArrayBuffer(LINE_SIZE);
        this.codecState = CodecState.CONTENT_READY; 
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
        this.codecState = CodecState.CONTENT_READY;
        
        iosession.setEventMask(SelectionKey.OP_WRITE);
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
                int bytesRead = this.contentBuf.fill(this.contentChannel);
                if (this.contentBuf.readLine(this.lineBuf, bytesRead == -1)) {
                    if (this.lineBuf.length() > 0 && this.lineBuf.charAt(0) == '.') {
                        buf.write(PERIOD);
                    }
                    buf.writeLine(this.lineBuf);
                    this.lineBuf.clear();
                }
                if (bytesRead == -1 && !this.contentBuf.hasData()) {
                    
                    this.lineBuf.append('.');
                    buf.writeLine(this.lineBuf);
                    this.lineBuf.clear();
                    
                    this.content.finish();
                    this.codecState = CodecState.CONTENT_RESPONSE_EXPECTED;
                    break;
                }
                if (bytesRead <= 0) {
                    break;
                }
            }
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
    
    public boolean isCompleted() {
        return this.codecState == CodecState.COMPLETED; 
    }

    public String next(
            final ProtocolCodecs<SessionState> codecs, 
            final SessionState sessionState) {
        if (isCompleted()) {
            return ProtocolState.QUIT.name();
        } else {
            return null;
        }
    }
        
}
