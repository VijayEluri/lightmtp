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
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;

import com.ok2c.lightnio.SessionInputBuffer;
import com.ok2c.lightnio.buffer.CharArrayBuffer;

/**
 * Decorator class intended to transparently extend 
 * an {@link SessionInputBuffer} with basic event logging capabilities using 
 * Commons Logging. 
 */
class LoggingSessionInputBuffer implements SessionInputBuffer {

    private final SessionInputBuffer buf;
    private final Wire wire;    
    private final Charset charset;

    public LoggingSessionInputBuffer(
            final SessionInputBuffer buf, 
            final Wire wire, 
            final Charset charset) {
        super();
        if (buf == null) {
            throw new IllegalArgumentException("Session input buffer may not be null");
        }
        this.buf = buf;
        this.wire = wire;
        this.charset = charset;
    }

    public int fill(final ReadableByteChannel src) throws IOException {
        return this.buf.fill(src);
    }

    public boolean hasData() {
        return this.buf.hasData();
    }

    public int length() {
        return this.buf.length();
    }

    public int available() {
        return this.buf.available();
    }

    public int capacity() {
        return this.buf.capacity();
    }

    public int read() {
        int b = this.buf.read();
        if (this.wire.isEnabled()) {
            this.wire.input(b);
        }
        return b;
    }

    public int read(final ByteBuffer dst, int maxLen) {
        if (this.wire.isEnabled()) {
            int chunk = Math.min(dst.remaining(), maxLen);
            ByteBuffer b = ByteBuffer.allocate(chunk); 
            int bytesRead = this.buf.read(b);
            b.flip();
            dst.put(b);
            b.rewind();
            this.wire.input(b);
            return bytesRead;
        } else {
            return this.buf.read(dst, maxLen);
        }
    }

    public int read(final ByteBuffer dst) {
        if (this.wire.isEnabled()) {
            ByteBuffer b = ByteBuffer.allocate(dst.remaining()); 
            int bytesRead = this.buf.read(b);
            b.flip();
            dst.put(b);
            b.rewind();
            this.wire.input(b);
            return bytesRead;
        } else {
            return this.buf.read(dst);
        }
    }

    public int read(final WritableByteChannel dst, int maxLen) throws IOException {
        if (this.wire.isEnabled()) {
            int chunk = Math.min(2048, maxLen);
            ByteBuffer b = ByteBuffer.allocate(chunk); 
            int bytesRead = this.buf.read(b);
            b.flip();
            while (b.hasRemaining()) {
                dst.write(b);
            }
            b.rewind();
            this.wire.input(b);
            return bytesRead;
        } else {
            return this.buf.read(dst, maxLen);
        }
    }

    public int read(final WritableByteChannel dst) throws IOException {
        if (this.wire.isEnabled()) {
            ByteBuffer b = ByteBuffer.allocate(2048); 
            int bytesRead = this.buf.read(b);
            b.flip();
            while (b.hasRemaining()) {
                dst.write(b);
            }
            b.rewind();
            this.wire.input(b);
            return bytesRead;
        } else {
            return this.buf.read(dst);
        }
    }

    public String readLine(boolean endOfStream) throws CharacterCodingException {
        String s = this.buf.readLine(endOfStream);
        if (this.wire.isEnabled()) {
            try {
                this.wire.input((s + "\r\n").getBytes(this.charset.name()));
            } catch (UnsupportedEncodingException ex) {
                throw new CharacterCodingException(); 
            }
        }
        return s;
    }

    public boolean readLine(
            final CharArrayBuffer dst, boolean endOfStream) throws CharacterCodingException {
        int pos = dst.length();
        boolean lineComplete = this.buf.readLine(dst, endOfStream);
        if (this.wire.isEnabled()) {
            String s = new String(dst.buffer(), pos, dst.length() - pos);
            try {
                this.wire.input((s + "\r\n").getBytes(this.charset.name()));
            } catch (UnsupportedEncodingException ex) {
                throw new CharacterCodingException(); 
            }
        }
        return lineComplete;
    }
    
}