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

import java.io.IOException;
import java.util.List;
import java.util.Set;

import com.ok2c.lightmtp.SMTPCode;
import com.ok2c.lightmtp.SMTPExtensions;
import com.ok2c.lightmtp.SMTPProtocolException;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightnio.SessionOutputBuffer;
import com.ok2c.lightnio.buffer.CharArrayBuffer;

public class SMTPReplyWriter implements SMTPMessageWriter<SMTPReply> {

    private final SessionOutputBuffer sessBuffer;
    private final CharArrayBuffer lineBuf;

    private boolean useEnhancedCodes;
    
    public SMTPReplyWriter(final SessionOutputBuffer sessBuffer) {
        super();
        if (sessBuffer == null) {
            throw new IllegalArgumentException("Session output buffer may not be null");
        }
        this.sessBuffer = sessBuffer;
        this.lineBuf = new CharArrayBuffer(1024);
        this.useEnhancedCodes = false;
    }
    
    public void upgrade(final Set<String> extensions) {
        if (extensions != null) {
            this.useEnhancedCodes = extensions.contains(SMTPExtensions.ENHANCEDSTATUSCODES);
        }
    }

    public void reset() {
        this.lineBuf.clear();
    }

    public void write(final SMTPReply message) throws IOException, SMTPProtocolException {
        if (message == null) {
            throw new IllegalArgumentException("Reply may not be null");
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
            this.sessBuffer.writeLine(this.lineBuf);
        }
    }
    
}
