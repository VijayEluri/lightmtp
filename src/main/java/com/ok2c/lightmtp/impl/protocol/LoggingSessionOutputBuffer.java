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

import com.ok2c.lightnio.SessionOutputBuffer;
import com.ok2c.lightnio.buffer.CharArrayBuffer;

/**
 * Decorator class intended to transparently extend 
 * an {@link SessionOutputBuffer} with basic event logging capabilities using 
 * Commons Logging. 
 */
class LoggingSessionOutputBuffer implements SessionOutputBuffer {

    private final SessionOutputBuffer buf;
    private final Wire wire;    
    private final Charset charset;
    
    public LoggingSessionOutputBuffer(
            final SessionOutputBuffer buf,
            final Wire wire, 
            final Charset charset) {
        super();
        if (buf == null) {
            throw new IllegalArgumentException("Session output buffer may not be null");
        }
        this.buf = buf;
        this.wire = wire;
        this.charset = charset;
    }

    public int flush(final WritableByteChannel channel) throws IOException {
        return this.buf.flush(channel);
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

    public void write(final ByteBuffer src) {
        if (this.wire.isEnabled()) {
            ByteBuffer b = src.duplicate();
            this.buf.write(src);
            this.wire.output(b);
        } else {
            this.buf.write(src);
        }
    }

    public void write(final ReadableByteChannel src) throws IOException {
        if (this.wire.isEnabled()) {
            ByteBuffer b = ByteBuffer.allocate(2048);
            src.read(b);
            b.flip();
            this.buf.write(b);
            b.rewind();
            this.wire.output(b);
        } else {
            this.buf.write(src);
        }
    }

    public void writeLine(final CharArrayBuffer src) throws CharacterCodingException {
        this.buf.writeLine(src);
        if (this.wire.isEnabled() && src != null) {
            String s = new String(src.buffer(), 0, src.length());
            try {
                this.wire.output((s + "\r\n").getBytes(this.charset.name()));
            } catch (UnsupportedEncodingException ex) {
                throw new CharacterCodingException(); 
            }
        }
    }

    public void writeLine(String s) throws IOException {
        this.buf.writeLine(s);
        if (this.wire.isEnabled() && s != null) {
            try {
                this.wire.output((s + "\r\n").getBytes(this.charset.name()));
            } catch (UnsupportedEncodingException ex) {
                throw new CharacterCodingException(); 
            }
        }
    }
    
}