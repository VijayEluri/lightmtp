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
package com.ok2c.lightmtp.impl.agent;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

import com.ok2c.lightmtp.SMTPConsts;
import com.ok2c.lightmtp.message.SMTPContent;

final class ContentUtils {

    public static String readToString(
            final SMTPContent<ReadableByteChannel> content) throws IOException {
        StringBuilder buffer = new StringBuilder();
        ReadableByteChannel channel = content.channel();
        try {
            CharsetDecoder decoder = SMTPConsts.ASCII.newDecoder();
            ByteBuffer dst = ByteBuffer.allocate(1024);
            CharBuffer chars = CharBuffer.allocate(1024);
            int len;
            while ((len = channel.read(dst)) != -1) {
                dst.flip();
                CoderResult result = decoder.decode(dst, chars, len == -1);
                if (result.isError()) {
                    result.throwException();
                }
                dst.compact();
                chars.flip();
                buffer.append(chars);
                chars.compact();
            }
        } finally {
            content.reset();
        }
        return buffer.toString();
    }

}
