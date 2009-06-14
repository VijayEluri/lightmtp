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
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.CharacterCodingException;
import java.util.LinkedList;
import java.util.Set;

import com.ok2c.lightmtp.SMTPCommand;
import com.ok2c.lightmtp.SMTPProtocolException;
import com.ok2c.lightnio.SessionInputBuffer;
import com.ok2c.lightnio.buffer.CharArrayBuffer;

public class SMTPCommandParser implements SMTPMessageParser<SMTPCommand> {

    private final SessionInputBuffer sessBuffer;
    private final CharArrayBuffer lineBuf;

    private boolean endOfStream;
    
    public SMTPCommandParser(final SessionInputBuffer sessBuffer) {
        super();
        if (sessBuffer == null) {
            throw new IllegalArgumentException("Session input buffer may not be null");
        }
        this.sessBuffer = sessBuffer;
        this.lineBuf = new CharArrayBuffer(1024);
        this.endOfStream = false;
    }
    
    public void upgrade(final Set<String> extensions) {
    }

    public void reset() {
        this.lineBuf.clear();
        this.endOfStream = false;
    }
    
    public int fillBuffer(final ReadableByteChannel channel) throws IOException {
        int bytesRead = this.sessBuffer.fill(channel);
        if (bytesRead == -1) {
            this.endOfStream = true;
        }
        return bytesRead;
    }
    
    public SMTPCommand parse() throws SMTPProtocolException {
        if (readLine()) {
            LinkedList<String> lines = new LinkedList<String>();
            int i = 0;
            int len = this.lineBuf.length();
            while (i < len) {
                if (this.lineBuf.charAt(i) != ' ') {
                    break;
                }
                i++;
            }
            while (i < len) {
                int idx = this.lineBuf.indexOf(' ', i, len);
                if (idx == -1) {
                    idx = len;
                }
                String s = this.lineBuf.substringTrimmed(i, idx);
                if (s.length() == 0) {
                    throw new SMTPProtocolException("Malformed command line: " 
                            + this.lineBuf.toString());
                }
                i += s.length() + 1;
                lines.add(s);
            }
            if (lines.isEmpty()) {
                throw new SMTPProtocolException("Empty command line");
            }
            String code = lines.removeFirst();
            String argument = null;
            if (!lines.isEmpty()) {
                argument = lines.removeFirst();
            }
            return new SMTPCommand(code, argument, lines); 
        } else {
            return null;
        }
    }
    
    private boolean readLine() throws SMTPProtocolException {
        try {
            return this.sessBuffer.readLine(this.lineBuf, this.endOfStream);
        } catch (CharacterCodingException ex) {
            throw new SMTPProtocolException("Invalid character coding", ex);
        }
    }
    
}
