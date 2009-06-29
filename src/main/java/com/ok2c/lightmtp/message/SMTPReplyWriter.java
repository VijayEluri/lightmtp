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
package com.ok2c.lightmtp.message;

import java.nio.charset.CharacterCodingException;
import java.util.List;

import com.ok2c.lightmtp.SMTPCode;
import com.ok2c.lightmtp.SMTPConsts;
import com.ok2c.lightmtp.SMTPProtocolException;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightnio.SessionOutputBuffer;
import com.ok2c.lightnio.buffer.CharArrayBuffer;

public class SMTPReplyWriter implements SMTPMessageWriter<SMTPReply> {

    private final CharArrayBuffer lineBuf;
    private final int maxLineLen;
    private final boolean useEnhancedCodes;
    
    public SMTPReplyWriter(int maxLineLen, boolean useEnhancedCodes) {
        super();
        this.lineBuf = new CharArrayBuffer(1024);
        this.maxLineLen = maxLineLen;
        this.useEnhancedCodes = useEnhancedCodes;
    }
    
    public SMTPReplyWriter(boolean useEnhancedCodes) {
        this(SMTPConsts.MAX_REPLY_LEN, useEnhancedCodes);
    }
    
    public SMTPReplyWriter() {
        this(false);
    }
    
    public void reset() {
        this.lineBuf.clear();
    }

    public void write(
            final SMTPReply message,
            final SessionOutputBuffer buf) throws SMTPProtocolException {
        if (message == null) {
            throw new IllegalArgumentException("Reply may not be null");
        }
        if (buf == null) {
            throw new IllegalArgumentException("Session output buffer may not be null");
        }
        List<String> lines = message.getLines();
        for (int i = 0; i < lines.size(); i++) {
            this.lineBuf.clear();
            this.lineBuf.append(Integer.toString(message.getCode()));
            if (i + 1 == lines.size()) {
                this.lineBuf.append(' ');
            } else {
                this.lineBuf.append('-');
            }
            if (this.useEnhancedCodes && message.getEnhancedCode() != null) {
                SMTPCode ec = message.getEnhancedCode();
                this.lineBuf.append(Integer.toString(ec.getCodeClass()));
                this.lineBuf.append('.');
                this.lineBuf.append(Integer.toString(ec.getSubject()));
                this.lineBuf.append('.');
                this.lineBuf.append(Integer.toString(ec.getDetail()));
                this.lineBuf.append(' ');
            }
            this.lineBuf.append(lines.get(i));
            writeLine(buf);
        }
    }
    
    private void writeLine(final SessionOutputBuffer buf) throws SMTPProtocolException {
        try {
            if (this.maxLineLen > 0 && this.lineBuf.length() > this.maxLineLen) {
                throw new SMTPProtocolException("Maximum reply length limit exceeded");
            }
            buf.writeLine(this.lineBuf);
        } catch (CharacterCodingException ex) {
            throw new SMTPProtocolException("Invalid character coding", ex);
        }
    }
    
}
