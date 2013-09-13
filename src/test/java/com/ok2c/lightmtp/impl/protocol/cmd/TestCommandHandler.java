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

import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Test;

import org.apache.http.concurrent.FutureCallback;

import com.ok2c.lightmtp.SMTPCode;
import com.ok2c.lightmtp.SMTPErrorException;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.impl.agent.SimpleEnvelopValidator;
import com.ok2c.lightmtp.impl.agent.SimpleIdGenerator;
import com.ok2c.lightmtp.impl.protocol.ClientType;
import com.ok2c.lightmtp.impl.protocol.DataType;
import com.ok2c.lightmtp.impl.protocol.MIMEEncoding;
import com.ok2c.lightmtp.impl.protocol.ServerState;
import com.ok2c.lightmtp.protocol.Action;

public class TestCommandHandler {

    @Test
    public void testHeloHandlerBasicResponse() throws Exception {
        ServerState state = new ServerState("whatever");
        HeloHandler handler = new HeloHandler();
        Action<ServerState> action = handler.handle("somedomain.com", null);
        Future<SMTPReply> future = action.execute(state, null);
        SMTPReply reply = future.get();

        Assert.assertNotNull(reply);
        Assert.assertEquals(250, reply.getCode());
        Assert.assertNull(reply.getEnhancedCode());
        Assert.assertEquals(ClientType.BASIC, state.getClientType());
        Assert.assertEquals("somedomain.com", state.getClientDomain());
    }

    @Test
    public void testHeloHandlerStateReset() throws Exception {
        ServerState state = new ServerState("whatever");
        state.setSender("someone@somewhere");
        state.getRecipients().add("someoneelse@somewhere");
        HeloHandler handler = new HeloHandler();
        Action<ServerState> action = handler.handle("somedomain.com", null);
        Future<SMTPReply> future = action.execute(state, null);
        SMTPReply reply = future.get();
        Assert.assertNotNull(reply);
        Assert.assertEquals(250, reply.getCode());
        Assert.assertNull(reply.getEnhancedCode());
        Assert.assertEquals(ClientType.BASIC, state.getClientType());
        Assert.assertEquals("somedomain.com", state.getClientDomain());
        Assert.assertNull(state.getSender());
        Assert.assertTrue(state.getRecipients().isEmpty());
        Assert.assertNull(state.getDataType());
    }

    @Test
    public void testHeloHandlerDomainNotGiven() throws Exception {
        HeloHandler handler = new HeloHandler();
        try {
            handler.handle(null, null);
        } catch (SMTPErrorException ex) {
            Assert.assertEquals(500, ex.getCode());
            Assert.assertNull(ex.getEnhancedCode());
        }
    }

    @Test
    public void testEhloHandlerBasicResponse() throws Exception {
        ServerState state = new ServerState("whatever");
        EhloHandler handler = new EhloHandler();
        Action<ServerState> action = handler.handle("somedomain.com", null);
        Future<SMTPReply> future = action.execute(state, null);
        SMTPReply reply = future.get();
        Assert.assertNotNull(reply);
        Assert.assertEquals(250, reply.getCode());
        Assert.assertNull(reply.getEnhancedCode());
        Assert.assertEquals(ClientType.EXTENDED, state.getClientType());
        Assert.assertEquals("somedomain.com", state.getClientDomain());
    }

    @Test
    public void testEhloHandlerStateReset() throws Exception {
        ServerState state = new ServerState("whatever");
        state.setSender("someone@somewhere");
        state.getRecipients().add("someoneelse@somewhere");
        EhloHandler handler = new EhloHandler();
        Action<ServerState> action = handler.handle("somedomain.com", null);
        Future<SMTPReply> future = action.execute(state, null);
        SMTPReply reply = future.get();
        Assert.assertNotNull(reply);
        Assert.assertEquals(250, reply.getCode());
        Assert.assertNull(reply.getEnhancedCode());
        Assert.assertEquals(ClientType.EXTENDED, state.getClientType());
        Assert.assertEquals("somedomain.com", state.getClientDomain());
        Assert.assertNull(state.getSender());
        Assert.assertTrue(state.getRecipients().isEmpty());
        Assert.assertNull(state.getDataType());
    }

    @Test
    public void testEhloHandlerDomainNotGiven() throws Exception {
        EhloHandler handler = new EhloHandler();
        try {
            handler.handle(null, null);
        } catch (SMTPErrorException ex) {
            Assert.assertEquals(500, ex.getCode());
            Assert.assertEquals(new SMTPCode(5, 5, 2), ex.getEnhancedCode());
        }
    }

    @Test
    public void testRsetHandlerBasicResponse() throws Exception {
        ServerState state = new ServerState("whatever");
        state.setSender("someone@somewhere");
        state.getRecipients().add("someoneelse@somewhere");
        RsetHandler handler = new RsetHandler();
        Action<ServerState> action = handler.handle(null, null);
        Future<SMTPReply> future = action.execute(state, null);
        SMTPReply reply = future.get();
        Assert.assertNotNull(reply);
        Assert.assertEquals(250, reply.getCode());
        Assert.assertEquals(new SMTPCode(2, 0, 0), reply.getEnhancedCode());
        Assert.assertNull(state.getSender());
        Assert.assertTrue(state.getRecipients().isEmpty());
        Assert.assertNull(state.getDataType());
    }

    @Test
    public void testNoopHandlerBasicResponse() throws Exception {
        ServerState state = new ServerState("whatever");
        state.setClientType(ClientType.BASIC);
        RsetHandler handler = new RsetHandler();
        Action<ServerState> action = handler.handle(null, null);
        Future<SMTPReply> future = action.execute(state, null);
        SMTPReply reply = future.get();
        Assert.assertNotNull(reply);
        Assert.assertEquals(250, reply.getCode());
        Assert.assertEquals(new SMTPCode(2, 0, 0), reply.getEnhancedCode());
    }

    @Test
    public void testMailFromHandlerBasicResponse() throws Exception {
        ServerState state = new ServerState("whatever");
        state.setClientType(ClientType.BASIC);
        MailFromHandler handler = new MailFromHandler(
                new SimpleIdGenerator(), new SimpleEnvelopValidator());
        Action<ServerState> action = handler.handle("from:<someone@somedomain.com>", null);
        Future<SMTPReply> future = action.execute(state, null);
        SMTPReply reply = future.get();
        Assert.assertNotNull(reply);
        Assert.assertEquals(250, reply.getCode());
        Assert.assertEquals(new SMTPCode(2, 1, 0), reply.getEnhancedCode());
        Assert.assertEquals("someone@somedomain.com", state.getSender());
        Assert.assertEquals(MIMEEncoding.MIME_7BIT, state.getMimeEncoding());
    }

    @Test
    public void testMailFromHandlerClientTypeNotKnown() throws Exception {
        ServerState state = new ServerState("whatever");
        state.setClientType(null);
        MailFromHandler handler = new MailFromHandler(new SimpleIdGenerator(), null);
        try {
            handler.handle("from:<someone@somedomain.com>", null);
        } catch (SMTPErrorException ex) {
            Assert.assertEquals(503, ex.getCode());
            Assert.assertEquals(new SMTPCode(5, 5, 1), ex.getEnhancedCode());
            Assert.assertNull(state.getSender());
        }
    }

    @Test
    public void testMailFromHandlerSenderSet() throws Exception {
        ServerState state = new ServerState("whatever");
        state.setSender("someone-else@somedomain.com");
        MailFromHandler handler = new MailFromHandler(new SimpleIdGenerator(), null);
        try {
            handler.handle("from:<someone@somedomain.com>", null);
        } catch (SMTPErrorException ex) {
            Assert.assertEquals(503, ex.getCode());
            Assert.assertEquals(new SMTPCode(5, 5, 1), ex.getEnhancedCode());
            Assert.assertEquals("someone-else@somedomain.com", state.getSender());
        }
    }

    @Test
    public void testMailFromHandlerInvalidArgument() throws Exception {
        ServerState state = new ServerState("whatever");
        state.setClientType(ClientType.BASIC);
        MailFromHandler handler = new MailFromHandler(new SimpleIdGenerator(), null);
        try {
            handler.handle("from:me", null);
        } catch (SMTPErrorException ex) {
            Assert.assertEquals(500, ex.getCode());
            Assert.assertEquals(new SMTPCode(5, 5, 1), ex.getEnhancedCode());
        }
    }

    @Test
    public void testMailFromHandler7BitMime() throws Exception {
        ServerState state = new ServerState("whatever");
        state.setClientType(ClientType.EXTENDED);
        MailFromHandler handler = new MailFromHandler(
                new SimpleIdGenerator(), new SimpleEnvelopValidator());
        Action<ServerState> action = handler.handle("from:<someone@somedomain.com> ",
                Arrays.asList("body=7bit"));
        Future<SMTPReply> future = action.execute(state, null);
        SMTPReply reply = future.get();
        Assert.assertNotNull(reply);
        Assert.assertEquals(250, reply.getCode());
        Assert.assertEquals(new SMTPCode(2, 1, 0), reply.getEnhancedCode());
        Assert.assertEquals("someone@somedomain.com", state.getSender());
        Assert.assertEquals(MIMEEncoding.MIME_7BIT, state.getMimeEncoding());
    }

    @Test
    public void testMailFromHandler8BitMime() throws Exception {
        ServerState state = new ServerState("whatever");
        state.setClientType(ClientType.EXTENDED);
        MailFromHandler handler = new MailFromHandler(
                new SimpleIdGenerator(), new SimpleEnvelopValidator());
        Action<ServerState> action = handler.handle("from:<someone@somedomain.com> ",
                Arrays.asList("body=8bitmime"));
        Future<SMTPReply> future = action.execute(state, null);
        SMTPReply reply = future.get();
        Assert.assertNotNull(reply);
        Assert.assertEquals(250, reply.getCode());
        Assert.assertEquals(new SMTPCode(2, 1, 0), reply.getEnhancedCode());
        Assert.assertEquals("someone@somedomain.com", state.getSender());
        Assert.assertEquals(MIMEEncoding.MIME_8BIT, state.getMimeEncoding());
    }

    @Test
    public void testMailFromHandlerInvalidParam1() throws Exception {
        ServerState state = new ServerState("whatever");
        state.setClientType(ClientType.EXTENDED);
        MailFromHandler handler = new MailFromHandler(new SimpleIdGenerator(), null);
        try {
            handler.handle("from:<someone@somedomain.com> ",
                    Arrays.asList("whatever"));
        } catch (SMTPErrorException ex) {
            Assert.assertEquals(501, ex.getCode());
            Assert.assertEquals(new SMTPCode(5, 5, 4), ex.getEnhancedCode());
        }
    }

    @Test
    public void testMailFromHandlerInvalidParam2() throws Exception {
        ServerState state = new ServerState("whatever");
        state.setClientType(ClientType.EXTENDED);
        MailFromHandler handler = new MailFromHandler(new SimpleIdGenerator(), null);
        try {
            handler.handle("from:<someone@somedomain.com> ",
                    Arrays.asList("body=whatever"));
        } catch (SMTPErrorException ex) {
            Assert.assertEquals(501, ex.getCode());
            Assert.assertEquals(new SMTPCode(5, 5, 4), ex.getEnhancedCode());
        }
    }

    @Test
    public void testMailFromHandlerInvalidParam3() throws Exception {
        ServerState state = new ServerState("whatever");
        state.setClientType(ClientType.EXTENDED);
        MailFromHandler handler = new MailFromHandler(new SimpleIdGenerator(), null);
        try {
            handler.handle("from:<someone@somedomain.com> ",
                    Arrays.asList("body=7bit", "whatever"));
        } catch (SMTPErrorException ex) {
            Assert.assertEquals(501, ex.getCode());
            Assert.assertEquals(new SMTPCode(5, 5, 4), ex.getEnhancedCode());
        }
    }

    @Test
    public void testRcptToHandlerBasicResponse() throws Exception {
        ServerState state = new ServerState("whatever");
        state.setClientType(ClientType.BASIC);
        state.setSender("me@somedomain.com");
        RcptToHandler handler = new RcptToHandler(new SimpleEnvelopValidator());
        Action<ServerState> action = handler.handle("to:<someone@somedomain.com>", null);
        Future<SMTPReply> future = action.execute(state, null);
        SMTPReply reply = future.get();
        Assert.assertNotNull(reply);
        Assert.assertEquals(250, reply.getCode());
        Assert.assertEquals(new SMTPCode(2, 1, 5), reply.getEnhancedCode());
        Assert.assertEquals("me@somedomain.com", state.getSender());
        Assert.assertEquals(1, state.getRecipients().size());
        Assert.assertEquals("someone@somedomain.com", state.getRecipients().get(0));

        action = handler.handle("to:<someone-else@somedomain.com>", null);
        future = action.execute(state, null);
        reply = future.get();
        Assert.assertNotNull(reply);
        Assert.assertEquals(250, reply.getCode());
        Assert.assertEquals(new SMTPCode(2, 1, 5), reply.getEnhancedCode());
        Assert.assertEquals("me@somedomain.com", state.getSender());
        Assert.assertEquals(2, state.getRecipients().size());
        Assert.assertEquals("someone-else@somedomain.com", state.getRecipients().get(1));
    }

    @Test
    public void testRcptToHandlerClientTypeNotKnown() throws Exception {
        ServerState state = new ServerState("whatever");
        state.setClientType(null);
        RcptToHandler handler = new RcptToHandler(null);
        try {
            handler.handle("to:<someone@somedomain.com>", null);
        } catch (SMTPErrorException ex) {
            Assert.assertEquals(503, ex.getCode());
            Assert.assertEquals(new SMTPCode(5, 5, 1), ex.getEnhancedCode());
            Assert.assertNull(state.getSender());
        }
    }

    @Test
    public void testRcptToHandlerSenderNotSet() throws Exception {
        ServerState state = new ServerState("whatever");
        state.setClientType(ClientType.BASIC);
        state.setSender(null);
        RcptToHandler handler = new RcptToHandler(null);
        try {
            handler.handle("to:<someone@somedomain.com>", null);
        } catch (SMTPErrorException ex) {
            Assert.assertEquals(503, ex.getCode());
            Assert.assertEquals(new SMTPCode(5, 5, 1), ex.getEnhancedCode());
        }
    }

    @Test
    public void testRcptToHandlerInvalidArgument() throws Exception {
        ServerState state = new ServerState("whatever");
        state.setClientType(ClientType.BASIC);
        state.setSender("me@somedomain.com");
        RcptToHandler handler = new RcptToHandler(null);
        try {
            handler.handle("to:me", null);
        } catch (SMTPErrorException ex) {
            Assert.assertEquals(500, ex.getCode());
            Assert.assertEquals(new SMTPCode(5, 5, 1), ex.getEnhancedCode());
        }
    }

    @Test
    public void testDataHandlerBasicResponse() throws Exception {
        ServerState state = new ServerState("whatever");
        state.setClientType(ClientType.BASIC);
        state.setSender("me@somedomain.com");
        state.getRecipients().add("someone@somedomain.com");
        DataHandler handler = new DataHandler();
        Action<ServerState> action = handler.handle(null, null);
        Future<SMTPReply> future = action.execute(state, null);
        SMTPReply reply = future.get();
        Assert.assertNotNull(reply);
        Assert.assertEquals(354, reply.getCode());
        Assert.assertNull(reply.getEnhancedCode());
        Assert.assertEquals(DataType.MIME, state.getDataType());
    }

    @Test
    public void testDataHandlerNoEnhancedResponse() throws Exception {
        ServerState state = new ServerState("whatever");
        state.setClientType(ClientType.EXTENDED);
        state.setSender("me@somedomain.com");
        state.getRecipients().add("someone@somedomain.com");
        DataHandler handler = new DataHandler();
        Action<ServerState> action = handler.handle(null, null);
        Future<SMTPReply> future = action.execute(state, null);
        SMTPReply reply = future.get();
        Assert.assertNotNull(reply);
        Assert.assertEquals(354, reply.getCode());
        Assert.assertNull(reply.getEnhancedCode());
        Assert.assertEquals(DataType.MIME, state.getDataType());
    }

    @Test
    public void testDataHandlerClientTypeNotKnown() throws Exception {
        ServerState state = new ServerState("whatever");
        state.setClientType(null);
        DataHandler handler = new DataHandler();
        try {
            handler.handle(null, null);
        } catch (SMTPErrorException ex) {
            Assert.assertEquals(503, ex.getCode());
            Assert.assertEquals(new SMTPCode(5, 5, 1), ex.getEnhancedCode());
            Assert.assertNull(state.getSender());
            Assert.assertNull(state.getDataType());
        }
    }

    @Test
    public void testDataHandlerSenderNotSet() throws Exception {
        ServerState state = new ServerState("whatever");
        state.setClientType(ClientType.BASIC);
        state.setSender(null);
        DataHandler handler = new DataHandler();
        try {
            handler.handle(null, null);
        } catch (SMTPErrorException ex) {
            Assert.assertEquals(503, ex.getCode());
            Assert.assertEquals(new SMTPCode(5, 5, 1), ex.getEnhancedCode());
            Assert.assertNull(state.getDataType());
        }
    }

    @Test
    public void testDataHandlerNoRecipients() throws Exception {
        ServerState state = new ServerState("whatever");
        state.setClientType(ClientType.BASIC);
        state.setSender("me@somedomain.com");
        DataHandler handler = new DataHandler();
        try {
            handler.handle(null, null);
        } catch (SMTPErrorException ex) {
            Assert.assertEquals(503, ex.getCode());
            Assert.assertEquals(new SMTPCode(5, 5, 1), ex.getEnhancedCode());
            Assert.assertNull(state.getDataType());
        }
    }

    @Test
    public void testVrfyHandlerByAddress() throws Exception {
        ServerState state = new ServerState("whatever");
        state.setClientType(ClientType.BASIC);
        VrfyHandler handler = new VrfyHandler(new SimpleEnvelopValidator() {

            @Override
            public Future<SMTPReply> validateRecipient(
                    final InetAddress client,
                    final String recipient,
                    final FutureCallback<SMTPReply> callback) {
                Assert.assertEquals("someaddress", recipient);
                return super.validateRecipient(client, recipient, callback);
            }

        });

        Action<ServerState> action = handler.handle("Some name <someaddress>", null);
        Future<SMTPReply> future = action.execute(state, null);
        SMTPReply reply = future.get();
        Assert.assertNotNull(reply);
        Assert.assertEquals(250, reply.getCode());
        Assert.assertEquals(new SMTPCode(2, 1, 5), reply.getEnhancedCode());
    }

    @Test
    public void testVrfyHandlerByFullInput() throws Exception {
        ServerState state = new ServerState("whatever");
        state.setClientType(ClientType.BASIC);
        VrfyHandler handler = new VrfyHandler(new SimpleEnvelopValidator() {

            @Override
            public Future<SMTPReply> validateRecipient(
                    final InetAddress client,
                    final String recipient,
                    final FutureCallback<SMTPReply> callback) {
                Assert.assertEquals("Some name <someaddress", recipient);
                return super.validateRecipient(client, recipient, callback);
            }

        });

        Action<ServerState> action = handler.handle("Some name <someaddress", null);
        Future<SMTPReply> future = action.execute(state, null);
        SMTPReply reply = future.get();
        Assert.assertNotNull(reply);
        Assert.assertEquals(250, reply.getCode());
        Assert.assertEquals(new SMTPCode(2, 1, 5), reply.getEnhancedCode());
    }

}
