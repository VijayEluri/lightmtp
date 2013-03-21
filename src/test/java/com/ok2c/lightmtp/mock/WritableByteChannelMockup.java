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
package com.ok2c.lightmtp.mock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;

public class WritableByteChannelMockup implements WritableByteChannel {

    private final ByteArrayOutputStream buffer;
    private final WritableByteChannel channel;
    private final Charset charset;

    private boolean closed;

    public WritableByteChannelMockup(final Charset charset) {
        super();
        this.buffer = new ByteArrayOutputStream();
        this.channel = Channels.newChannel(this.buffer);
        this.charset = charset;
        this.closed = false;
    }

    @Override
    public int write(final ByteBuffer src) throws IOException {
        if (this.closed) {
            throw new ClosedChannelException();
        }
        return this.channel.write(src);
    }

    @Override
    public void close() throws IOException {
        this.closed = true;
    }

    @Override
    public boolean isOpen() {
        return !this.closed;
    }

    public String getContent() {
        byte[] raw = this.buffer.toByteArray();
        try {
            return new String(raw, this.charset.name());
        } catch (UnsupportedEncodingException ex) {
            return new String(raw);
        }
    }

}
