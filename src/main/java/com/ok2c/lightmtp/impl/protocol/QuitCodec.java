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

import com.ok2c.lightmtp.SMTPCommand;
import com.ok2c.lightmtp.SMTPProtocolException;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.message.SMTPCommandWriter;
import com.ok2c.lightmtp.message.SMTPMessageParser;
import com.ok2c.lightmtp.message.SMTPMessageWriter;
import com.ok2c.lightmtp.message.SMTPReplyParser;
import com.ok2c.lightmtp.protocol.ProtocolCodec;
import com.ok2c.lightmtp.protocol.ProtocolCodecs;
import com.ok2c.lightnio.IOSession;
import com.ok2c.lightnio.SessionInputBuffer;
import com.ok2c.lightnio.SessionOutputBuffer;

public class QuitCodec implements ProtocolCodec<SessionState> {
    
    enum CodecState {
        
        QUIT_READY,
        QUIT_RESPONSE_EXPECTED,
        COMPLETED
        
    }
    
    private final SMTPMessageParser<SMTPReply> parser;
    private final SMTPMessageWriter<SMTPCommand> writer;
    
    private CodecState codecState;
    
    public QuitCodec() {
        super();
        this.parser = new SMTPReplyParser();
        this.writer = new SMTPCommandWriter();
        this.codecState = CodecState.QUIT_READY; 
    }

    public void reset(
            final IOSession iosession, 
            final SessionState sessionState) throws IOException, SMTPProtocolException {
        this.parser.reset();
        this.writer.reset();
        this.codecState = CodecState.QUIT_READY;
        
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
        case QUIT_READY:
            SMTPCommand quit = new SMTPCommand("QUIT");
            this.writer.write(quit, buf);
            this.codecState = CodecState.QUIT_RESPONSE_EXPECTED;
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
            case QUIT_RESPONSE_EXPECTED:
                iosession.close();
                this.codecState = CodecState.COMPLETED;
                sessionState.setReply(reply);
                break;
            default:
                throw new SMTPProtocolException("Unexpected reply: " + reply);
            }
        }
    }
    
    public boolean isCompleted() {
        return this.codecState == CodecState.COMPLETED; 
    }

    public boolean isIdle() {
        return this.codecState == CodecState.QUIT_READY; 
    }

    public String next(
            final ProtocolCodecs<SessionState> codecs, 
            final SessionState sessionState) {
        return null;
    }
        
}
