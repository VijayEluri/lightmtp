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

import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Test;

import com.ok2c.lightmtp.SMTPCode;
import com.ok2c.lightmtp.SMTPCommand;
import com.ok2c.lightmtp.SMTPErrorException;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.impl.protocol.ClientType;
import com.ok2c.lightmtp.impl.protocol.ServerState;
import com.ok2c.lightmtp.protocol.Action;

public class TestDefaultProtocolHandler {

    @Test
    public void testDefaultProtocolHandlerBasics() throws Exception {
        ServerState state = new ServerState("whatever");

        DefaultProtocolHandler handler = new DefaultProtocolHandler();
        handler.register("HELO", new HeloHandler());
        handler.register("NOOP", new NoopHandler());

        SMTPCommand cmd1 = new SMTPCommand("HELO", "somedomain.com");

        Action<ServerState> action1 = handler.handle(cmd1);
        Future<SMTPReply> future1 = action1.execute(state, null);
        SMTPReply reply1 = future1.get();

        Assert.assertNotNull(reply1);
        Assert.assertEquals(250, reply1.getCode());
        Assert.assertNull(reply1.getEnhancedCode());
        Assert.assertEquals(ClientType.BASIC, state.getClientType());
        Assert.assertEquals("somedomain.com", state.getClientDomain());

        SMTPCommand cmd2 = new SMTPCommand("NOOP");

        Action<ServerState> action2 = handler.handle(cmd2);
        Future<SMTPReply> future2 = action2.execute(state, null);
        SMTPReply reply2 = future2.get();
        Assert.assertNotNull(reply2);
        Assert.assertEquals(250, reply2.getCode());
        Assert.assertEquals(new SMTPCode(2, 0, 0), reply2.getEnhancedCode());

        SMTPCommand cmd3 = new SMTPCommand("WHATEVER");

        try {
            handler.handle(cmd3);
        } catch (SMTPErrorException ex) {
            Assert.assertEquals(500, ex.getCode());
            Assert.assertEquals(new SMTPCode(5, 5, 1), ex.getEnhancedCode());
        }

        handler.unregister("HELO");

        SMTPCommand cmd4 = new SMTPCommand("HELO", "somedomain.com");

        try {
            handler.handle(cmd4);
        } catch (SMTPErrorException ex) {
            Assert.assertEquals(500, ex.getCode());
            Assert.assertEquals(new SMTPCode(5, 5, 1), ex.getEnhancedCode());
        }
    }

    @Test
    public void testDefaultProtocolHandlerCaseInsensitive() throws Exception {
        ServerState state = new ServerState("whatever");

        DefaultProtocolHandler handler = new DefaultProtocolHandler();
        handler.register("HELO", new HeloHandler());
        handler.register("NOOP", new NoopHandler());

        SMTPCommand cmd1 = new SMTPCommand("Helo", "somedomain.com");

        Action<ServerState> action1 = handler.handle(cmd1);
        Future<SMTPReply> future1 = action1.execute(state, null);
        SMTPReply reply1 = future1.get();
        Assert.assertNotNull(reply1);
        Assert.assertEquals(250, reply1.getCode());
        Assert.assertNull(reply1.getEnhancedCode());
        Assert.assertEquals(ClientType.BASIC, state.getClientType());
        Assert.assertEquals("somedomain.com", state.getClientDomain());

        SMTPCommand cmd2 = new SMTPCommand("NoOp");

        Action<ServerState> action2 = handler.handle(cmd2);
        Future<SMTPReply> future2 = action2.execute(state, null);
        SMTPReply reply2 = future2.get();
        Assert.assertNotNull(reply2);
        Assert.assertEquals(250, reply2.getCode());
        Assert.assertEquals(new SMTPCode(2, 0, 0), reply2.getEnhancedCode());
    }

}
