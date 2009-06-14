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

import org.junit.Test;

import com.ok2c.lightmtp.SMTPCommand;
import com.ok2c.lightmtp.mock.WritableByteChannelMockup;
import com.ok2c.lightnio.SessionOutputBuffer;
import com.ok2c.lightnio.impl.SessionOutputBufferImpl;

public class TestSMTPCommandWriter {

    private final static Charset ASCII = Charset.forName("ASCII");
    
    @Test
    public void testConstructor() throws Exception {
        try {
            new SMTPCommandWriter(null);
            Assert.fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException expected) {
        }
        SessionOutputBuffer outbuf = new SessionOutputBufferImpl(4096, 1024, ASCII); 
        SMTPMessageWriter<SMTPCommand> writer = new SMTPCommandWriter(outbuf);
        writer.reset();
    }

    @Test
    public void testBasicCommandWriting() throws Exception {
        SessionOutputBuffer outbuf = new SessionOutputBufferImpl(4096, 1024, ASCII); 
        SMTPMessageWriter<SMTPCommand> writer = new SMTPCommandWriter(outbuf);

        SMTPCommand cmd = new SMTPCommand("NOOP");
        writer.write(cmd);
         
        WritableByteChannelMockup channel = new WritableByteChannelMockup(ASCII);
        outbuf.flush(channel);

        String content = channel.getContent();
        Assert.assertEquals("NOOP\r\n", content);
    }

    @Test
    public void testCommandWithArgWriting() throws Exception {
        SessionOutputBuffer outbuf = new SessionOutputBufferImpl(4096, 1024, ASCII); 
        SMTPMessageWriter<SMTPCommand> writer = new SMTPCommandWriter(outbuf);

        SMTPCommand cmd = new SMTPCommand("MAIL", "FROM:<someone@pampa.com>");
        writer.write(cmd);
         
        WritableByteChannelMockup channel = new WritableByteChannelMockup(ASCII);
        outbuf.flush(channel);

        String content = channel.getContent();
        Assert.assertEquals("MAIL FROM:<someone@pampa.com>\r\n", content);
    }

    @Test
    public void testCommandWithArgAndParamsWriting() throws Exception {
        SessionOutputBuffer outbuf = new SessionOutputBufferImpl(4096, 1024, ASCII); 
        SMTPMessageWriter<SMTPCommand> writer = new SMTPCommandWriter(outbuf);

        SMTPCommand cmd = new SMTPCommand("MAIL", "FROM:<someone@pampa.com>",
                new ArrayList<String>(Arrays.asList("THIS", "AND", "THAT")));
        writer.write(cmd);
         
        WritableByteChannelMockup channel = new WritableByteChannelMockup(ASCII);
        outbuf.flush(channel);

        String content = channel.getContent();
        Assert.assertEquals("MAIL FROM:<someone@pampa.com> THIS AND THAT\r\n", content);
    }

}
