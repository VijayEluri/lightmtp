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

import com.ok2c.lightmtp.SMTPCommand;
import com.ok2c.lightmtp.SMTPProtocolException;
import com.ok2c.lightnio.SessionOutputBuffer;
import com.ok2c.lightnio.buffer.CharArrayBuffer;

public class SMTPCommandWriter implements SMTPMessageWriter<SMTPCommand> {

    private final SessionOutputBuffer sessBuffer;
    private final CharArrayBuffer lineBuf;
    
    public SMTPCommandWriter(final SessionOutputBuffer sessBuffer) {
        super();
        if (sessBuffer == null) {
            throw new IllegalArgumentException("Session output buffer may not be null");
        }
        this.sessBuffer = sessBuffer;
        this.lineBuf = new CharArrayBuffer(1024);
    }
    
    public void upgrade(final Set<String> extensions) {
    }

    public void reset() {
        this.lineBuf.clear();
    }

    public void write(final SMTPCommand message) throws IOException, SMTPProtocolException {
        if (message == null) {
            throw new IllegalArgumentException("Command may not be null");
        }
        this.lineBuf.clear();
        this.lineBuf.append(message.getCode());
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
        this.sessBuffer.writeLine(this.lineBuf);
    }
    
}
