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

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

import com.ok2c.lightmtp.SMTPCode;
import com.ok2c.lightmtp.SMTPExtensions;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.mock.WritableByteChannelMockup;
import com.ok2c.lightnio.SessionOutputBuffer;
import com.ok2c.lightnio.impl.SessionOutputBufferImpl;

public class TestSMTPReplyWriter {

    private final static Charset ASCII = Charset.forName("ASCII");
    private final static Set<String> EXTS = new HashSet<String>(
            Arrays.asList(SMTPExtensions.ENHANCEDSTATUSCODES));
    
    @Test
    public void testConstructor() throws Exception {
        try {
            new SMTPCommandWriter(null);
            Assert.fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException expected) {
        }
        SessionOutputBuffer outbuf = new SessionOutputBufferImpl(4096, 1024, ASCII); 
        SMTPMessageWriter<SMTPReply> writer = new SMTPReplyWriter(outbuf);
        writer.reset();
    }

    @Test
    public void testBasicReplyWriting() throws Exception {
        SessionOutputBuffer outbuf = new SessionOutputBufferImpl(4096, 1024, ASCII); 
        SMTPMessageWriter<SMTPReply> writer = new SMTPReplyWriter(outbuf);

        SMTPReply reply = new SMTPReply(250, "OK");
        writer.write(reply);
         
        WritableByteChannelMockup channel = new WritableByteChannelMockup(ASCII);
        outbuf.flush(channel);

        String content = channel.getContent();
        Assert.assertEquals("250 OK\r\n", content);
    }

    @Test
    public void testReplyWithEnhancedCodeWriting() throws Exception {
        SessionOutputBuffer outbuf = new SessionOutputBufferImpl(4096, 1024, ASCII); 
        SMTPMessageWriter<SMTPReply> writer = new SMTPReplyWriter(outbuf);
        writer.upgrade(EXTS);

        SMTPReply reply = new SMTPReply(250, new SMTPCode(2, 5, 0), "OK");
        writer.write(reply);
         
        WritableByteChannelMockup channel = new WritableByteChannelMockup(ASCII);
        outbuf.flush(channel);

        String content = channel.getContent();
        Assert.assertEquals("250 2.5.0 OK\r\n", content);
    }

    @Test
    public void testMultilineReplyWriting() throws Exception {
        SessionOutputBuffer outbuf = new SessionOutputBufferImpl(4096, 1024, ASCII); 
        SMTPMessageWriter<SMTPReply> writer = new SMTPReplyWriter(outbuf);

        SMTPReply reply = new SMTPReply(250, null,
                new ArrayList<String>(
                        Arrays.asList("whatever.com", "PIPELINING", "ENHANCEDSTATUSCODES")));
        writer.write(reply);
         
        WritableByteChannelMockup channel = new WritableByteChannelMockup(ASCII);
        outbuf.flush(channel);

        String content = channel.getContent();
        Assert.assertEquals(
                "250-whatever.com\r\n" +
                "250-PIPELINING\r\n" +
                "250 ENHANCEDSTATUSCODES\r\n", content);
    }

    @Test
    public void testMultilineReplyWithEnhancedCodeWriting() throws Exception {
        SessionOutputBuffer outbuf = new SessionOutputBufferImpl(4096, 1024, ASCII); 
        SMTPMessageWriter<SMTPReply> writer = new SMTPReplyWriter(outbuf);
        writer.upgrade(EXTS);

        SMTPReply reply = new SMTPReply(250, new SMTPCode(2, 5, 0),
                new ArrayList<String>(
                        Arrays.asList("whatever.com", "PIPELINING", "ENHANCEDSTATUSCODES")));
        writer.write(reply);
         
        WritableByteChannelMockup channel = new WritableByteChannelMockup(ASCII);
        outbuf.flush(channel);

        String content = channel.getContent();
        Assert.assertEquals(
                "250-2.5.0 whatever.com\r\n" +
                "250-2.5.0 PIPELINING\r\n" +
                "250 2.5.0 ENHANCEDSTATUSCODES\r\n", content);
    }

}
