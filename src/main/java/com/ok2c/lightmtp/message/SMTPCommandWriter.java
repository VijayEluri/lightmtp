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

import com.ok2c.lightmtp.SMTPCommand;
import com.ok2c.lightmtp.SMTPConsts;
import com.ok2c.lightmtp.SMTPProtocolException;
import com.ok2c.lightnio.SessionOutputBuffer;
import com.ok2c.lightnio.buffer.CharArrayBuffer;

public class SMTPCommandWriter implements SMTPMessageWriter<SMTPCommand> {

    private final CharArrayBuffer lineBuf;
    private final int maxLineLen;
    
    public SMTPCommandWriter(int maxLineLen) {
        super();
        this.lineBuf = new CharArrayBuffer(1024);
        this.maxLineLen = maxLineLen;
    }
    
    public SMTPCommandWriter() {
        this(SMTPConsts.MAX_COMMAND_LEN);
    }
    
    public void reset() {
        this.lineBuf.clear();
    }

    public void write(
            final SMTPCommand message, 
            final SessionOutputBuffer buf) throws SMTPProtocolException {
        if (message == null) {
            throw new IllegalArgumentException("Command may not be null");
        }
        if (buf == null) {
            throw new IllegalArgumentException("Session output buffer may not be null");
        }
        this.lineBuf.clear();
        this.lineBuf.append(message.getVerb());
        if (message.getArgument() != null) {
            this.lineBuf.append(' ');
            this.lineBuf.append(message.getArgument());
            List<String> params = message.getParams();
            if (params != null) {
                for (String param: params) {
                    this.lineBuf.append(' ');
                    this.lineBuf.append(param);
                }
            }
        }
        writeLine(buf);
    }
    
    private void writeLine(final SessionOutputBuffer buf) throws SMTPProtocolException {
        try {
            if (this.maxLineLen > 0 && this.lineBuf.length() > this.maxLineLen) {
                throw new SMTPProtocolException("Maximum command length limit exceeded");
            }
            buf.writeLine(this.lineBuf);
        } catch (CharacterCodingException ex) {
            throw new SMTPProtocolException("Invalid character coding", ex);
        }
    }
    
}
