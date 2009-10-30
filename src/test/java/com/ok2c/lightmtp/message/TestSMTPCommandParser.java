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
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII); 
        SMTPMessageParser<SMTPCommand> parser = new SMTPCommandParser();
        Assert.assertNull(parser.parse(inbuf, false));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testInvalidConstructorParam() throws Exception {
        SMTPMessageParser<SMTPCommand> parser = new SMTPCommandParser();
        parser.parse(null, false);
    }

    @Test
    public void testBasicCommandParsing() throws Exception {
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII); 
        SMTPMessageParser<SMTPCommand> parser = new SMTPCommandParser();
        
        String[] input = new String[] {
                "NOOP\r\n"
        };
        
        ReadableByteChannel channel = new ReadableByteChannelMockup(input, ASCII);
        inbuf.fill(channel);
        SMTPCommand command = parser.parse(inbuf, false);
        Assert.assertNotNull(command);
        Assert.assertEquals("NOOP", command.getVerb());
        Assert.assertNull(command.getArgument());
        Assert.assertNotNull(command.getParams());
        Assert.assertEquals(0, command.getParams().size());
    }

    @Test
    public void testCommandParsingEndOfStream() throws Exception {
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII); 
        SMTPMessageParser<SMTPCommand> parser = new SMTPCommandParser();
        
        String[] input = new String[] {
                "NOOP"
        };
        
        ReadableByteChannel channel = new ReadableByteChannelMockup(input, ASCII);
        inbuf.fill(channel);
        Assert.assertNull(parser.parse(inbuf, false));
        SMTPCommand command = parser.parse(inbuf, true);
        Assert.assertNotNull(command);
        Assert.assertEquals("NOOP", command.getVerb());
        Assert.assertNull(command.getArgument());
        Assert.assertNotNull(command.getParams());
        Assert.assertEquals(0, command.getParams().size());
    }

    @Test
    public void testComplexCommandParsing() throws Exception {
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII); 
        SMTPMessageParser<SMTPCommand> parser = new SMTPCommandParser();
        
        String[] input = new String[] {
                "MAIL FROM:<someone@pampa.com> BODY=8BITMIME THIS AND THAT\r\n"
        };
        
        ReadableByteChannel channel = new ReadableByteChannelMockup(input, ASCII);
        inbuf.fill(channel);
        SMTPCommand command = parser.parse(inbuf, false);
        Assert.assertNotNull(command);
        Assert.assertEquals("MAIL", command.getVerb());
        Assert.assertEquals("FROM:<someone@pampa.com>", command.getArgument());
        Assert.assertNotNull(command.getParams());
        Assert.assertEquals(4, command.getParams().size());
        Assert.assertEquals("BODY=8BITMIME", command.getParams().get(0));
    }

    @Test
    public void testChunkedCommandParsing() throws Exception {
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII); 
        SMTPMessageParser<SMTPCommand> parser = new SMTPCommandParser();
        
        String[] input = new String[] {
                "MAIL FROM",
                ":<someone@pamp",
                "a.com> BODY=8BITMI",
                "ME\r\n"
        };
        
        ReadableByteChannel channel = new ReadableByteChannelMockup(input, ASCII);
        inbuf.fill(channel);
        Assert.assertNull(parser.parse(inbuf, false));
        inbuf.fill(channel);
        Assert.assertNull(parser.parse(inbuf, false));
        inbuf.fill(channel);
        Assert.assertNull(parser.parse(inbuf, false));
        inbuf.fill(channel);
        SMTPCommand command = parser.parse(inbuf, false);
        Assert.assertNotNull(command);
        Assert.assertEquals("MAIL", command.getVerb());
        Assert.assertEquals("FROM:<someone@pampa.com>", command.getArgument());
        Assert.assertNotNull(command.getParams());
        Assert.assertEquals(1, command.getParams().size());
        Assert.assertEquals("BODY=8BITMIME", command.getParams().get(0));
    }
    
    @Test(expected=SMTPProtocolException.class)
    public void testEmptyCommandParsing() throws Exception {
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII); 
        SMTPMessageParser<SMTPCommand> parser = new SMTPCommandParser();
        
        String[] input = new String[] {
                "   \r\n"
        };
        
        ReadableByteChannel channel = new ReadableByteChannelMockup(input, ASCII);
        inbuf.fill(channel);
        parser.parse(inbuf, false);
    }

    public void testLenientCommandParsingWithLeadingBlanks() throws Exception {
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII); 
        SMTPMessageParser<SMTPCommand> parser = new SMTPCommandParser();
        
        String[] input = new String[] {
                "  MAIL FROM:<someone@pampa.com>\r\n"
        };
        
        ReadableByteChannel channel = new ReadableByteChannelMockup(input, ASCII);
        inbuf.fill(channel);
        SMTPCommand command = parser.parse(inbuf, false);
        Assert.assertNotNull(command);
        Assert.assertEquals("MAIL", command.getVerb());
        Assert.assertEquals("FROM:<someone@pampa.com>", command.getArgument());
    }


    @Test(expected=SMTPProtocolException.class)
    public void testOverMaxLenCommandParsing() throws Exception {
        SessionInputBuffer inbuf = new SessionInputBufferImpl(4096, 1024, ASCII); 
        SMTPMessageParser<SMTPCommand> parser = new SMTPCommandParser(16);
        
        String[] input = new String[] {
                "BLAHBLAHBLAHBLAHBLAH\r\n"
        };
        
        ReadableByteChannel channel = new ReadableByteChannelMockup(input, ASCII);
        inbuf.fill(channel);
        parser.parse(inbuf, false);
    }

}
