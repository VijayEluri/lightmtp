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
package com.ok2c.lightmtp.impl.protocol.cmd;

import junit.framework.Assert;

import org.junit.Test;

import com.ok2c.lightmtp.SMTPCode;
import com.ok2c.lightmtp.SMTPCommand;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.impl.protocol.ClientType;
import com.ok2c.lightmtp.impl.protocol.ServerSessionState;

public class TestDefaultProtocolHandler {

    @Test
    public void testDefaultProtocolHandlerBasics() throws Exception {
        ServerSessionState state = new ServerSessionState("whatever");

        DefaultProtocolHandler handler = new DefaultProtocolHandler();
        handler.register("HELO", new HeloHandler());
        handler.register("NOOP", new NoopHandler());

        SMTPCommand cmd1 = new SMTPCommand("HELO", "somedomain.com");

        SMTPReply reply1 = handler.handle(cmd1, state);
        Assert.assertNotNull(reply1);
        Assert.assertEquals(250, reply1.getCode());
        Assert.assertNull(reply1.getEnhancedCode());
        Assert.assertEquals(ClientType.BASIC, state.getClientType());
        Assert.assertEquals("somedomain.com", state.getClientDomain());

        SMTPCommand cmd2 = new SMTPCommand("NOOP");

        SMTPReply reply2 = handler.handle(cmd2, state);
        Assert.assertNotNull(reply2);
        Assert.assertEquals(250, reply2.getCode());
        Assert.assertNull(reply2.getEnhancedCode());

        SMTPCommand cmd3 = new SMTPCommand("WHATEVER");

        SMTPReply reply3 = handler.handle(cmd3, state);
        Assert.assertNotNull(reply3);
        Assert.assertEquals(500, reply3.getCode());
        Assert.assertNull(reply3.getEnhancedCode());

        handler.unregister("HELO");

        SMTPCommand cmd4 = new SMTPCommand("HELO", "somedomain.com");

        SMTPReply reply4 = handler.handle(cmd4, state);
        Assert.assertNotNull(reply4);
        Assert.assertEquals(500, reply4.getCode());
        Assert.assertNull(reply4.getEnhancedCode());
    }

    @Test
    public void testDefaultProtocolHandlerBasicsEnhancedCode() throws Exception {
        ServerSessionState state = new ServerSessionState("whatever");

        DefaultProtocolHandler handler = new DefaultProtocolHandler();
        handler.register("EHLO", new EhloHandler());
        handler.register("NOOP", new NoopHandler());

        SMTPCommand cmd1 = new SMTPCommand("EHLO", "somedomain.com");

        SMTPReply reply1 = handler.handle(cmd1, state);
        Assert.assertNotNull(reply1);
        Assert.assertEquals(250, reply1.getCode());
        Assert.assertEquals(new SMTPCode(2, 0, 0), reply1.getEnhancedCode());
        Assert.assertEquals(ClientType.EXTENDED, state.getClientType());
        Assert.assertEquals("somedomain.com", state.getClientDomain());

        SMTPCommand cmd2 = new SMTPCommand("NOOP");

        SMTPReply reply2 = handler.handle(cmd2, state);
        Assert.assertNotNull(reply2);
        Assert.assertEquals(new SMTPCode(2, 0, 0), reply2.getEnhancedCode());
        Assert.assertEquals(250, reply2.getCode());

        SMTPCommand cmd3 = new SMTPCommand("WHATEVER");

        SMTPReply reply3 = handler.handle(cmd3, state);
        Assert.assertNotNull(reply3);
        Assert.assertEquals(500, reply3.getCode());
        Assert.assertEquals(new SMTPCode(5, 5, 1), reply3.getEnhancedCode());

        handler.unregister("EHLO");

        SMTPCommand cmd4 = new SMTPCommand("EHLO", "somedomain.com");

        SMTPReply reply4 = handler.handle(cmd4, state);
        Assert.assertNotNull(reply4);
        Assert.assertEquals(500, reply4.getCode());
        Assert.assertEquals(new SMTPCode(5, 5, 1), reply4.getEnhancedCode());
    }

    @Test
    public void testDefaultProtocolHandlerCaseInsensitive() throws Exception {
        ServerSessionState state = new ServerSessionState("whatever");

        DefaultProtocolHandler handler = new DefaultProtocolHandler();
        handler.register("HELO", new HeloHandler());
        handler.register("NOOP", new NoopHandler());

        SMTPCommand cmd1 = new SMTPCommand("Helo", "somedomain.com");

        SMTPReply reply1 = handler.handle(cmd1, state);
        Assert.assertNotNull(reply1);
        Assert.assertEquals(250, reply1.getCode());
        Assert.assertNull(reply1.getEnhancedCode());
        Assert.assertEquals(ClientType.BASIC, state.getClientType());
        Assert.assertEquals("somedomain.com", state.getClientDomain());

        SMTPCommand cmd2 = new SMTPCommand("NoOp");

        SMTPReply reply2 = handler.handle(cmd2, state);
        Assert.assertNotNull(reply2);
        Assert.assertEquals(250, reply2.getCode());
        Assert.assertNull(reply2.getEnhancedCode());
    }
}
