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

import org.junit.Assert;
import org.junit.Test;

import org.apache.http.impl.nio.reactor.SessionOutputBufferImpl;
import org.apache.http.nio.reactor.SessionOutputBuffer;

import com.ok2c.lightmtp.SMTPCommand;
import com.ok2c.lightmtp.SMTPProtocolException;
import com.ok2c.lightmtp.mock.WritableByteChannelMockup;

public class TestSMTPCommandWriter {

    private final static Charset ASCII = Charset.forName("ASCII");

    @Test
    public void testConstructor() throws Exception {
        SMTPMessageWriter<SMTPCommand> writer = new SMTPCommandWriter();
        writer.reset();
    }

    @Test(expected=IllegalArgumentException.class)
    public void testInvalidConstructorParam1() throws Exception {
        SMTPMessageWriter<SMTPCommand> writer = new SMTPCommandWriter();
        SessionOutputBuffer outbuf = new SessionOutputBufferImpl(4096, 1024, ASCII);
        writer.write(null, outbuf);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testInvalidConstructorParam2() throws Exception {
        SMTPMessageWriter<SMTPCommand> writer = new SMTPCommandWriter();
        writer.write(new SMTPCommand("NOOP"), null);
    }

    @Test
    public void testBasicCommandWriting() throws Exception {
        SessionOutputBuffer outbuf = new SessionOutputBufferImpl(4096, 1024, ASCII);
        SMTPMessageWriter<SMTPCommand> writer = new SMTPCommandWriter();

        SMTPCommand cmd = new SMTPCommand("NOOP");
        writer.write(cmd, outbuf);

        WritableByteChannelMockup channel = new WritableByteChannelMockup(ASCII);
        outbuf.flush(channel);

        String content = channel.getContent();
        Assert.assertEquals("NOOP\r\n", content);
    }

    @Test
    public void testCommandWithArgWriting() throws Exception {
        SessionOutputBuffer outbuf = new SessionOutputBufferImpl(4096, 1024, ASCII);
        SMTPMessageWriter<SMTPCommand> writer = new SMTPCommandWriter();

        SMTPCommand cmd = new SMTPCommand("MAIL", "FROM:<someone@pampa.com>");
        writer.write(cmd, outbuf);

        WritableByteChannelMockup channel = new WritableByteChannelMockup(ASCII);
        outbuf.flush(channel);

        String content = channel.getContent();
        Assert.assertEquals("MAIL FROM:<someone@pampa.com>\r\n", content);
    }

    @Test
    public void testCommandWithArgAndParamsWriting() throws Exception {
        SessionOutputBuffer outbuf = new SessionOutputBufferImpl(4096, 1024, ASCII);
        SMTPMessageWriter<SMTPCommand> writer = new SMTPCommandWriter();

        SMTPCommand cmd = new SMTPCommand("MAIL", "FROM:<someone@pampa.com>",
                new ArrayList<String>(Arrays.asList("THIS", "AND", "THAT")));
        writer.write(cmd, outbuf);

        WritableByteChannelMockup channel = new WritableByteChannelMockup(ASCII);
        outbuf.flush(channel);

        String content = channel.getContent();
        Assert.assertEquals("MAIL FROM:<someone@pampa.com> THIS AND THAT\r\n", content);
    }

    @Test(expected=SMTPProtocolException.class)
    public void testOverMaxLenCommandWriting() throws Exception {
        SessionOutputBuffer outbuf = new SessionOutputBufferImpl(4096, 1024, ASCII);
        SMTPMessageWriter<SMTPCommand> writer = new SMTPCommandWriter(16);

        SMTPCommand cmd = new SMTPCommand("BLAHBLAHBLAHBLAHBLAH");
        writer.write(cmd, outbuf);
    }

}
