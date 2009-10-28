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
package com.ok2c.lightmtp.impl;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.junit.Assert;
import org.junit.Test;

import com.ok2c.lightnio.SessionInputBuffer;
import com.ok2c.lightnio.buffer.CharArrayBuffer;

/**
 * Unit tests for {@link SMTPInputBuffer} and {@link SMTPOutputBuffer}.
 */
public class TestSMTPInOutBuffers {

    private static ReadableByteChannel newChannel(final String s, final String charset)
            throws UnsupportedEncodingException {
        return Channels.newChannel(new ByteArrayInputStream(s.getBytes(charset)));
    }

    private static ReadableByteChannel newChannel(final String s)
            throws UnsupportedEncodingException {
        return newChannel(s, "US-ASCII");
    }

    @Test
    public void testReadLineChunks() throws Exception {
        SessionInputBuffer inbuf = new SMTPInputBuffer(16, 16);

        ReadableByteChannel channel = newChannel("O\ne\r\nTwo\r\nTh\ree");

        inbuf.fill(channel);

        CharArrayBuffer line = new CharArrayBuffer(64);

        line.clear();
        Assert.assertTrue(inbuf.readLine(line, false));
        Assert.assertEquals("O\ne", line.toString());

        line.clear();
        Assert.assertTrue(inbuf.readLine(line, false));
        Assert.assertEquals("Two", line.toString());

        line.clear();
        Assert.assertFalse(inbuf.readLine(line, false));

        channel = newChannel("\r\nFour");
        inbuf.fill(channel);

        line.clear();
        Assert.assertTrue(inbuf.readLine(line, false));
        Assert.assertEquals("Th\ree", line.toString());

        inbuf.fill(channel);

        line.clear();
        Assert.assertTrue(inbuf.readLine(line, true));
        Assert.assertEquals("Four", line.toString());

        line.clear();
        Assert.assertFalse(inbuf.readLine(line, true));
    }

    @Test
    public void testInBufferClear() throws Exception {
        SMTPInputBuffer inbuf = new SMTPInputBuffer(16, 16);

        ReadableByteChannel channel = newChannel("yadayada\r\n");

        inbuf.fill(channel);
        Assert.assertEquals(10, inbuf.length());
        
        inbuf.clear();
        Assert.assertEquals(0, inbuf.length());
    }

    @Test
    public void testOutBufferClear() throws Exception {
        SMTPOutputBuffer outbuf = new SMTPOutputBuffer(16, 16);

        outbuf.writeLine("yadayada");
        
        Assert.assertEquals(10, outbuf.length());
        
        outbuf.clear();
        Assert.assertEquals(0, outbuf.length());
    }

}
