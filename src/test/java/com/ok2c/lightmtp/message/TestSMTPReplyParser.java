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

import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

import org.junit.Assert;
import org.junit.Test;

import org.apache.http.impl.nio.reactor.SessionInputBufferImpl;
import org.apache.http.nio.reactor.SessionInputBuffer;

import com.ok2c.lightmtp.SMTPCode;
import com.ok2c.lightmtp.SMTPProtocolException;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.mock.ReadableByteChannelMockup;

public class TestSMTPReplyParser {

    private final static Charset ASCII = Charset.forName("ASCII");

    @Test
    public void testConstructor() throws Exception {
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII);
        SMTPMessageParser<SMTPReply> parser = new SMTPReplyParser();
        Assert.assertNull(parser.parse(inbuf, false));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testInvalidConstructorParam() throws Exception {
        SMTPMessageParser<SMTPReply> parser = new SMTPReplyParser();
        parser.parse(null, false);
    }

    @Test
    public void testBasicReplyParsing() throws Exception {
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII);
        SMTPMessageParser<SMTPReply> parser = new SMTPReplyParser();

        String[] input = new String[] {
                "250 OK\r\n"
        };

        ReadableByteChannel channel = new ReadableByteChannelMockup(input, ASCII);
        inbuf.fill(channel);
        SMTPReply reply = parser.parse(inbuf, false);
        Assert.assertNotNull(reply);
        Assert.assertEquals(250, reply.getCode());
        Assert.assertNull(reply.getEnhancedCode());
        Assert.assertEquals("OK", reply.getLine());
    }

    @Test
    public void testMultipleReplyParsing() throws Exception {
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII);
        SMTPMessageParser<SMTPReply> parser = new SMTPReplyParser();

        String[] input = new String[] {
                "250 OK\r\n500 NOT OK\r\n550 NOT OK\r\n"
        };

        ReadableByteChannel channel = new ReadableByteChannelMockup(input, ASCII);
        inbuf.fill(channel);
        SMTPReply reply1 = parser.parse(inbuf, false);
        Assert.assertNotNull(reply1);
        Assert.assertEquals(250, reply1.getCode());
        SMTPReply reply2 = parser.parse(inbuf, false);
        Assert.assertNotNull(reply2);
        Assert.assertEquals(500, reply2.getCode());
        SMTPReply reply3 = parser.parse(inbuf, false);
        Assert.assertNotNull(reply3);
        Assert.assertEquals(550, reply3.getCode());
    }

    @Test
    public void testReplyParsingEndOfStream() throws Exception {
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII);
        SMTPMessageParser<SMTPReply> parser = new SMTPReplyParser();

        String[] input = new String[] {
                "250 OK"
        };

        ReadableByteChannel channel = new ReadableByteChannelMockup(input, ASCII);
        inbuf.fill(channel);
        Assert.assertNull(parser.parse(inbuf, false));
        SMTPReply reply = parser.parse(inbuf, true);
        Assert.assertNotNull(reply);
        Assert.assertEquals(250, reply.getCode());
        Assert.assertNull(reply.getEnhancedCode());
        Assert.assertEquals("OK", reply.getLine());
    }

    @Test
    public void testReplyParsingWithEnhancedCode() throws Exception {
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII);
        SMTPMessageParser<SMTPReply> parser = new SMTPReplyParser(true);
        Assert.assertNull(parser.parse(inbuf, false));

        String[] input = new String[] {
                "250 2.5.0 OK\r\n"
        };

        ReadableByteChannel channel = new ReadableByteChannelMockup(input, ASCII);
        inbuf.fill(channel);
        SMTPReply reply = parser.parse(inbuf, false);
        Assert.assertNotNull(reply);
        Assert.assertEquals(250, reply.getCode());
        Assert.assertEquals(new SMTPCode(2, 5, 0), reply.getEnhancedCode());
        Assert.assertEquals("OK", reply.getLine());
    }

    @Test
    public void testChunkedReplyParsing() throws Exception {
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII);
        SMTPMessageParser<SMTPReply> parser = new SMTPReplyParser();
        Assert.assertNull(parser.parse(inbuf, false));

        String[] input = new String[] {
                "25",
                "0 ",
                "blah ",
                "blah\r\n"
        };

        ReadableByteChannel channel = new ReadableByteChannelMockup(input, ASCII);
        inbuf.fill(channel);
        Assert.assertNull(parser.parse(inbuf, false));
        inbuf.fill(channel);
        Assert.assertNull(parser.parse(inbuf, false));
        inbuf.fill(channel);
        Assert.assertNull(parser.parse(inbuf, false));
        inbuf.fill(channel);
        SMTPReply reply = parser.parse(inbuf, false);
        Assert.assertNotNull(reply);
        Assert.assertEquals(250, reply.getCode());
        Assert.assertNull(reply.getEnhancedCode());
        Assert.assertEquals("blah blah", reply.getLine());
    }

    @Test
    public void testReplyParsingWithLeadingBlanks() throws Exception {
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII);
        SMTPMessageParser<SMTPReply> parser = new SMTPReplyParser();
        Assert.assertNull(parser.parse(inbuf, false));

        String[] input = new String[] {
                "   250 OK\r\n"
        };

        ReadableByteChannel channel = new ReadableByteChannelMockup(input, ASCII);
        inbuf.fill(channel);
        SMTPReply reply = parser.parse(inbuf, false);
        Assert.assertNotNull(reply);
        Assert.assertEquals(250, reply.getCode());
        Assert.assertNull(reply.getEnhancedCode());
        Assert.assertEquals("OK", reply.getLine());
    }

    @Test
    public void testMultilineReplyParsing() throws Exception {
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII);
        SMTPMessageParser<SMTPReply> parser = new SMTPReplyParser();
        Assert.assertNull(parser.parse(inbuf, false));

        String[] input = new String[] {
                "250-whatever.com\r\n",
                "250-PIPELINING\r\n",
                "250-ENHANCEDSTATUSCODES\r\n",
                "250 8BITMIME\r\n"
        };

        ReadableByteChannel channel = new ReadableByteChannelMockup(input, ASCII);
        inbuf.fill(channel);
        Assert.assertNull(parser.parse(inbuf, false));
        inbuf.fill(channel);
        Assert.assertNull(parser.parse(inbuf, false));
        inbuf.fill(channel);
        Assert.assertNull(parser.parse(inbuf, false));

        inbuf.fill(channel);
        SMTPReply reply = parser.parse(inbuf, false);

        Assert.assertEquals(250, reply.getCode());
        Assert.assertNull(reply.getEnhancedCode());
        Assert.assertNotNull(reply.getLines());
        Assert.assertEquals(4, reply.getLines().size());
        Assert.assertEquals("whatever.com", reply.getLines().get(0));
        Assert.assertEquals("PIPELINING", reply.getLines().get(1));
        Assert.assertEquals("ENHANCEDSTATUSCODES", reply.getLines().get(2));
        Assert.assertEquals("8BITMIME", reply.getLines().get(3));
    }

    @Test
    public void testMultilineReplyParsingWithEnhancedCode() throws Exception {
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII);
        SMTPMessageParser<SMTPReply> parser = new SMTPReplyParser(true);
        Assert.assertNull(parser.parse(inbuf, false));

        String[] input = new String[] {
                "250-2.5.0 whatever.com\r\n",
                "250-2.5.0 PIPELINING\r\n",
                "250-2.5.0 ENHANCEDSTATUSCODES\r\n",
                "250 2.5.0 8BITMIME\r\n"
        };

        ReadableByteChannel channel = new ReadableByteChannelMockup(input, ASCII);
        inbuf.fill(channel);
        Assert.assertNull(parser.parse(inbuf, false));
        inbuf.fill(channel);
        Assert.assertNull(parser.parse(inbuf, false));
        inbuf.fill(channel);
        Assert.assertNull(parser.parse(inbuf, false));

        inbuf.fill(channel);
        SMTPReply reply = parser.parse(inbuf, false);

        Assert.assertEquals(250, reply.getCode());
        Assert.assertEquals(new SMTPCode(2, 5, 0), reply.getEnhancedCode());
        Assert.assertNotNull(reply.getLines());
        Assert.assertEquals(4, reply.getLines().size());
        Assert.assertEquals("whatever.com", reply.getLines().get(0));
        Assert.assertEquals("PIPELINING", reply.getLines().get(1));
        Assert.assertEquals("ENHANCEDSTATUSCODES", reply.getLines().get(2));
        Assert.assertEquals("8BITMIME", reply.getLines().get(3));
    }

    @Test
    public void testChunkedMultilineReplyParsing() throws Exception {
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII);
        SMTPMessageParser<SMTPReply> parser = new SMTPReplyParser();
        Assert.assertNull(parser.parse(inbuf, false));

        String[] input = new String[] {
                "25",
                "0-wha",
                "tever.com",
                "\r\n",
                "250",
                "-PIP",
                "ELINING\r",
                "\n",
                "250",
                "-",
                "ENHANCEDSTATUSCODES\r\n",
                "250",
                " ",
                "8BITMIME\r\n"
        };

        ReadableByteChannel channel = new ReadableByteChannelMockup(input, ASCII);
        SMTPReply reply = null;
        while (reply == null) {
            inbuf.fill(channel);
            reply = parser.parse(inbuf, false);
        }
        Assert.assertEquals(250, reply.getCode());
        Assert.assertNull(reply.getEnhancedCode());
        Assert.assertNotNull(reply.getLines());
        Assert.assertEquals(4, reply.getLines().size());
        Assert.assertEquals("whatever.com", reply.getLines().get(0));
        Assert.assertEquals("PIPELINING", reply.getLines().get(1));
        Assert.assertEquals("ENHANCEDSTATUSCODES", reply.getLines().get(2));
        Assert.assertEquals("8BITMIME", reply.getLines().get(3));
    }

    @Test(expected=SMTPProtocolException.class)
    public void testParsingInvalidCode() throws Exception {
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII);
        SMTPMessageParser<SMTPReply> parser = new SMTPReplyParser();
        Assert.assertNull(parser.parse(inbuf, false));

        String[] input = new String[] {
                "2s0 OK\r\n"
        };

        ReadableByteChannel channel = new ReadableByteChannelMockup(input, ASCII);
        inbuf.fill(channel);
        parser.parse(inbuf, false);
    }

    @Test(expected=SMTPProtocolException.class)
    public void testParsingNoCode() throws Exception {
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII);
        SMTPMessageParser<SMTPReply> parser = new SMTPReplyParser();
        Assert.assertNull(parser.parse(inbuf, false));

        String[] input = new String[] {
                "    OK\r\n"
        };

        ReadableByteChannel channel = new ReadableByteChannelMockup(input, ASCII);
        inbuf.fill(channel);
        parser.parse(inbuf, false);
    }

    @Test(expected=SMTPProtocolException.class)
    public void testParsingInvalidCodeDelimiter() throws Exception {
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII);
        SMTPMessageParser<SMTPReply> parser = new SMTPReplyParser();
        Assert.assertNull(parser.parse(inbuf, false));

        String[] input = new String[] {
                "250=whatever.com\r\n",
                "250 PIPELINING\r\n"
        };

        ReadableByteChannel channel = new ReadableByteChannelMockup(input, ASCII);
        inbuf.fill(channel);
        parser.parse(inbuf, false);
    }

    @Test(expected=SMTPProtocolException.class)
    public void testParsingInvalidEnhancedCode1() throws Exception {
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII);
        SMTPMessageParser<SMTPReply> parser = new SMTPReplyParser(true);
        Assert.assertNull(parser.parse(inbuf, false));

        String[] input = new String[] {
                "250 dddddd OK\r\n"
        };

        ReadableByteChannel channel = new ReadableByteChannelMockup(input, ASCII);
        inbuf.fill(channel);
        parser.parse(inbuf, false);
    }

    @Test(expected=SMTPProtocolException.class)
    public void testParsingInvalidEnhancedCode2() throws Exception {
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII);
        SMTPMessageParser<SMTPReply> parser = new SMTPReplyParser(true);
        Assert.assertNull(parser.parse(inbuf, false));

        String[] input = new String[] {
                "250 d.5.0 OK\r\n"
        };

        ReadableByteChannel channel = new ReadableByteChannelMockup(input, ASCII);
        inbuf.fill(channel);
        parser.parse(inbuf, false);
    }

    @Test(expected=SMTPProtocolException.class)
    public void testParsingInvalidEnhancedCode3() throws Exception {
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII);
        SMTPMessageParser<SMTPReply> parser = new SMTPReplyParser(true);
        Assert.assertNull(parser.parse(inbuf, false));

        String[] input = new String[] {
                "250 2.dddd OK\r\n"
        };

        ReadableByteChannel channel = new ReadableByteChannelMockup(input, ASCII);
        inbuf.fill(channel);
        parser.parse(inbuf, false);
    }

    @Test(expected=SMTPProtocolException.class)
    public void testParsingInvalidEnhancedCode4() throws Exception {
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII);
        SMTPMessageParser<SMTPReply> parser = new SMTPReplyParser(true);
        Assert.assertNull(parser.parse(inbuf, false));

        String[] input = new String[] {
                "250 2.d.0 OK\r\n"
        };

        ReadableByteChannel channel = new ReadableByteChannelMockup(input, ASCII);
        inbuf.fill(channel);
        parser.parse(inbuf, false);
    }

    @Test(expected=SMTPProtocolException.class)
    public void testParsingInvalidEnhancedCode5() throws Exception {
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII);
        SMTPMessageParser<SMTPReply> parser = new SMTPReplyParser(true);
        Assert.assertNull(parser.parse(inbuf, false));

        String[] input = new String[] {
                "250 2.5.d OK\r\n"
        };

        ReadableByteChannel channel = new ReadableByteChannelMockup(input, ASCII);
        inbuf.fill(channel);
        parser.parse(inbuf, false);
    }

    @Test(expected=SMTPProtocolException.class)
    public void testParsingInvalidEnhancedCode6() throws Exception {
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII);
        SMTPMessageParser<SMTPReply> parser = new SMTPReplyParser(true);
        Assert.assertNull(parser.parse(inbuf, false));

        String[] input = new String[] {
                "250 2.5.0\r\n"
        };

        ReadableByteChannel channel = new ReadableByteChannelMockup(input, ASCII);
        inbuf.fill(channel);
        parser.parse(inbuf, false);
    }

    @Test(expected=SMTPProtocolException.class)
    public void testParsingInvalidCodeClassMismatch() throws Exception {
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII);
        SMTPMessageParser<SMTPReply> parser = new SMTPReplyParser(true);
        Assert.assertNull(parser.parse(inbuf, false));

        String[] input = new String[] {
                "250 3.5.0 OK\r\n"
        };

        ReadableByteChannel channel = new ReadableByteChannelMockup(input, ASCII);
        inbuf.fill(channel);
        parser.parse(inbuf, false);
    }

    @Test(expected=SMTPProtocolException.class)
    public void testOverMaxLenReplyParsing() throws Exception {
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII);
        SMTPMessageParser<SMTPReply> parser = new SMTPReplyParser(16, false);
        Assert.assertNull(parser.parse(inbuf, false));

        String[] input = new String[] {
                "200 BLAHBLAHBLAHBLAH\r\n"
        };

        ReadableByteChannel channel = new ReadableByteChannelMockup(input, ASCII);
        inbuf.fill(channel);
        parser.parse(inbuf, false);
    }

}
