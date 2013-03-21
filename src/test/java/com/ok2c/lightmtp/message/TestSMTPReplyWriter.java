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

import junit.framework.Assert;

import org.apache.http.impl.nio.reactor.SessionOutputBufferImpl;
import org.apache.http.nio.reactor.SessionOutputBuffer;
import org.junit.Test;

import com.ok2c.lightmtp.SMTPCode;
import com.ok2c.lightmtp.SMTPProtocolException;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.mock.WritableByteChannelMockup;

public class TestSMTPReplyWriter {

    private final static Charset ASCII = Charset.forName("ASCII");

    @Test
    public void testConstructor() throws Exception {
        SMTPMessageWriter<SMTPReply> writer = new SMTPReplyWriter();
        writer.reset();
    }

    @Test(expected=IllegalArgumentException.class)
    public void testInvalidConstructorParam1() throws Exception {
        SMTPMessageWriter<SMTPReply> writer = new SMTPReplyWriter();
        SessionOutputBuffer outbuf = new SessionOutputBufferImpl(4096, 1024, ASCII);
        writer.write(null, outbuf);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testInvalidConstructorParam2() throws Exception {
        SMTPMessageWriter<SMTPReply> writer = new SMTPReplyWriter();
        writer.write(new SMTPReply(250, null, "OK"), null);
    }

    @Test
    public void testBasicReplyWriting() throws Exception {
        SessionOutputBuffer outbuf = new SessionOutputBufferImpl(4096, 1024, ASCII);
        SMTPMessageWriter<SMTPReply> writer = new SMTPReplyWriter();

        SMTPReply reply = new SMTPReply(250, null, "OK");
        writer.write(reply, outbuf);

        WritableByteChannelMockup channel = new WritableByteChannelMockup(ASCII);
        outbuf.flush(channel);

        String content = channel.getContent();
        Assert.assertEquals("250 OK\r\n", content);
    }

    @Test
    public void testReplyWithEnhancedCodeWriting() throws Exception {
        SessionOutputBuffer outbuf = new SessionOutputBufferImpl(4096, 1024, ASCII);
        SMTPMessageWriter<SMTPReply> writer = new SMTPReplyWriter(true);

        SMTPReply reply = new SMTPReply(250, new SMTPCode(2, 5, 0), "OK");
        writer.write(reply, outbuf);

        WritableByteChannelMockup channel = new WritableByteChannelMockup(ASCII);
        outbuf.flush(channel);

        String content = channel.getContent();
        Assert.assertEquals("250 2.5.0 OK\r\n", content);
    }

    @Test
    public void testMultilineReplyWriting() throws Exception {
        SessionOutputBuffer outbuf = new SessionOutputBufferImpl(4096, 1024, ASCII);
        SMTPMessageWriter<SMTPReply> writer = new SMTPReplyWriter();

        SMTPReply reply = new SMTPReply(250, null,
                new ArrayList<String>(
                        Arrays.asList("whatever.com", "PIPELINING", "ENHANCEDSTATUSCODES")));
        writer.write(reply, outbuf);

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
        SMTPMessageWriter<SMTPReply> writer = new SMTPReplyWriter(true);

        SMTPReply reply = new SMTPReply(250, new SMTPCode(2, 5, 0),
                new ArrayList<String>(
                        Arrays.asList("whatever.com", "PIPELINING", "ENHANCEDSTATUSCODES")));
        writer.write(reply, outbuf);

        WritableByteChannelMockup channel = new WritableByteChannelMockup(ASCII);
        outbuf.flush(channel);

        String content = channel.getContent();
        Assert.assertEquals(
                "250-2.5.0 whatever.com\r\n" +
                "250-2.5.0 PIPELINING\r\n" +
                "250 2.5.0 ENHANCEDSTATUSCODES\r\n", content);
    }

    @Test(expected=SMTPProtocolException.class)
    public void testOverMaxLenReplyWriting() throws Exception {
        SessionOutputBuffer outbuf = new SessionOutputBufferImpl(4096, 1024, ASCII);
        SMTPMessageWriter<SMTPReply> writer = new SMTPReplyWriter(16, true);

        SMTPReply reply = new SMTPReply(250, null, "BLAHBLAHBLAHBLAH");
        writer.write(reply, outbuf);
    }

}
