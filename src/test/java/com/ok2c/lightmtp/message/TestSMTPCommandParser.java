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

import junit.framework.Assert;

import org.junit.Test;

import com.ok2c.lightmtp.SMTPCommand;
import com.ok2c.lightmtp.SMTPProtocolException;
import com.ok2c.lightmtp.message.SMTPCommandParser;
import com.ok2c.lightmtp.message.SMTPMessageParser;
import com.ok2c.lightmtp.mock.ReadableByteChannelMockup;
import com.ok2c.lightnio.SessionInputBuffer;
import com.ok2c.lightnio.impl.SessionInputBufferImpl;

public class TestSMTPCommandParser {

    private final static Charset ASCII = Charset.forName("ASCII");
    
    @Test
    public void testConstructor() throws Exception {
        try {
            new SMTPCommandParser(null);
            Assert.fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException expected) {
        }
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII); 
        SMTPMessageParser<SMTPCommand> parser = new SMTPCommandParser(inbuf);
        Assert.assertNull(parser.parse());
    }

    @Test
    public void testBasicCommandParsing() throws Exception {
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII); 
        SMTPMessageParser<SMTPCommand> parser = new SMTPCommandParser(inbuf);
        Assert.assertNull(parser.parse());
        
        String[] input = new String[] {
                "NOOP\r\n"
        };
        
        ReadableByteChannel channel = new ReadableByteChannelMockup(input, ASCII);
        parser.fillBuffer(channel);
        SMTPCommand command = parser.parse();
        Assert.assertNotNull(command);
        Assert.assertEquals("NOOP", command.getCode());
        Assert.assertNull(command.getArgument());
        Assert.assertNotNull(command.getParams());
        Assert.assertEquals(0, command.getParams().size());
    }

    @Test
    public void testComplexCommandParsing() throws Exception {
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII); 
        SMTPMessageParser<SMTPCommand> parser = new SMTPCommandParser(inbuf);
        Assert.assertNull(parser.parse());
        
        String[] input = new String[] {
                "MAIL FROM:<someone@pampa.com> BODY=8BITMIME THIS AND THAT\r\n"
        };
        
        ReadableByteChannel channel = new ReadableByteChannelMockup(input, ASCII);
        parser.fillBuffer(channel);
        SMTPCommand command = parser.parse();
        Assert.assertNotNull(command);
        Assert.assertEquals("MAIL", command.getCode());
        Assert.assertEquals("FROM:<someone@pampa.com>", command.getArgument());
        Assert.assertNotNull(command.getParams());
        Assert.assertEquals(4, command.getParams().size());
        Assert.assertEquals("BODY=8BITMIME", command.getParams().get(0));
    }

    @Test
    public void testChunkedCommandParsing() throws Exception {
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII); 
        SMTPMessageParser<SMTPCommand> parser = new SMTPCommandParser(inbuf);
        Assert.assertNull(parser.parse());
        
        String[] input = new String[] {
                "MAIL FROM",
                ":<someone@pamp",
                "a.com> BODY=8BITMI",
                "ME\r\n"
        };
        
        ReadableByteChannel channel = new ReadableByteChannelMockup(input, ASCII);
        parser.fillBuffer(channel);
        Assert.assertNull(parser.parse());
        parser.fillBuffer(channel);
        Assert.assertNull(parser.parse());
        parser.fillBuffer(channel);
        Assert.assertNull(parser.parse());
        parser.fillBuffer(channel);
        SMTPCommand command = parser.parse();
        Assert.assertNotNull(command);
        Assert.assertEquals("MAIL", command.getCode());
        Assert.assertEquals("FROM:<someone@pampa.com>", command.getArgument());
        Assert.assertNotNull(command.getParams());
        Assert.assertEquals(1, command.getParams().size());
        Assert.assertEquals("BODY=8BITMIME", command.getParams().get(0));
    }
    
    @Test(expected=SMTPProtocolException.class)
    public void testEmptyCommandParsing() throws Exception {
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII); 
        SMTPMessageParser<SMTPCommand> parser = new SMTPCommandParser(inbuf);
        Assert.assertNull(parser.parse());
        
        String[] input = new String[] {
                "   \r\n"
        };
        
        ReadableByteChannel channel = new ReadableByteChannelMockup(input, ASCII);
        parser.fillBuffer(channel);
        parser.parse();
    }

    public void testLenientCommandParsingWithLeadingBlanks() throws Exception {
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII); 
        SMTPMessageParser<SMTPCommand> parser = new SMTPCommandParser(inbuf);
        Assert.assertNull(parser.parse());
        
        String[] input = new String[] {
                "  MAIL FROM:<someone@pampa.com>\r\n"
        };
        
        ReadableByteChannel channel = new ReadableByteChannelMockup(input, ASCII);
        parser.fillBuffer(channel);
        SMTPCommand command = parser.parse();
        Assert.assertNotNull(command);
        Assert.assertEquals("MAIL", command.getCode());
        Assert.assertEquals("FROM:<someone@pampa.com>", command.getArgument());
    }

}
