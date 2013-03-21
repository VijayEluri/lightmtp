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
package com.ok2c.lightmtp.message.content;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import com.ok2c.lightmtp.message.SMTPContent;

public class ByteArraySource implements SMTPContent<ReadableByteChannel> {

    private final byte[] content;

    private InputStream stream;

    public ByteArraySource(final byte[] content) {
        super();
        this.content = content;
        this.stream = null;
    }

    @Override
    public ReadableByteChannel channel() {
        if (this.stream == null) {
            this.stream = new ByteArrayInputStream(this.content);
        }
        return Channels.newChannel(this.stream);
    }

    @Override
    public long length() {
        return this.content.length;
    }

    @Override
    public void reset() {
        this.stream = null;
    }

}
