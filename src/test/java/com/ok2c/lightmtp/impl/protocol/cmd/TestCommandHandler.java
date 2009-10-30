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
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.impl.protocol.ClientType;
import com.ok2c.lightmtp.impl.protocol.DataType;
import com.ok2c.lightmtp.impl.protocol.ServerSessionState;

public class TestCommandHandler {

    @Test
    public void testHeloHandlerBasicResponse() throws Exception {
        ServerSessionState state = new ServerSessionState("whatever");
        HeloHandler handler = new HeloHandler(null);
        SMTPReply reply = handler.handle("somedomain.com", null, state);
        Assert.assertNotNull(reply);
        Assert.assertEquals(250, reply.getCode());
        Assert.assertNull(reply.getEnhancedCode());
        Assert.assertEquals(ClientType.BASIC, state.getClientType());
        Assert.assertEquals("somedomain.com", state.getClientDomain());
    }

    @Test
    public void testHeloHandlerStateReset() throws Exception {
        ServerSessionState state = new ServerSessionState("whatever");
        state.setSender("someone@somewhere");
        state.getRecipients().add("someoneelse@somewhere");
        HeloHandler handler = new HeloHandler(null);
        SMTPReply reply = handler.handle("somedomain.com", null, state);
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
        ServerSessionState state = new ServerSessionState("whatever");
        HeloHandler handler = new HeloHandler(null);
        SMTPReply reply = handler.handle(null, null, state);
        Assert.assertNotNull(reply);
        Assert.assertEquals(500, reply.getCode());
        Assert.assertNull(reply.getEnhancedCode());
        Assert.assertNull(state.getClientType());
        Assert.assertNull(state.getClientDomain());
    }

    @Test
    public void testEhloHandlerBasicResponse() throws Exception {
        ServerSessionState state = new ServerSessionState("whatever");
        EhloHandler handler = new EhloHandler(null);
        SMTPReply reply = handler.handle("somedomain.com", null, state);
        Assert.assertNotNull(reply);
        Assert.assertEquals(250, reply.getCode());
        Assert.assertNull(reply.getEnhancedCode());
        Assert.assertEquals(ClientType.EXTENDED, state.getClientType());
        Assert.assertEquals("somedomain.com", state.getClientDomain());
    }

    @Test
    public void testEhloHandlerStateReset() throws Exception {
        ServerSessionState state = new ServerSessionState("whatever");
        state.setSender("someone@somewhere");
        state.getRecipients().add("someoneelse@somewhere");
        EhloHandler handler = new EhloHandler(null);
        SMTPReply reply = handler.handle("somedomain.com", null, state);
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
        ServerSessionState state = new ServerSessionState("whatever");
        EhloHandler handler = new EhloHandler(null);
        SMTPReply reply = handler.handle(null, null, state);
        Assert.assertNotNull(reply);
        Assert.assertEquals(500, reply.getCode());
        Assert.assertEquals(new SMTPCode(5, 5, 2), reply.getEnhancedCode());
        Assert.assertNull(state.getClientType());
        Assert.assertNull(state.getClientDomain());
    }

    @Test
    public void testRsetHandlerBasicResponse() throws Exception {
        ServerSessionState state = new ServerSessionState("whatever");
        state.setSender("someone@somewhere");
        state.getRecipients().add("someoneelse@somewhere");
        RsetHandler handler = new RsetHandler();
        SMTPReply reply = handler.handle(null, null, state);
        Assert.assertNotNull(reply);
        Assert.assertEquals(250, reply.getCode());
        Assert.assertNull(reply.getEnhancedCode());
        Assert.assertNull(state.getSender());
        Assert.assertTrue(state.getRecipients().isEmpty());
        Assert.assertNull(state.getDataType());
    }

    @Test
    public void testRsetHandlerEnhancedResponse() throws Exception {
        ServerSessionState state = new ServerSessionState("whatever");
        state.setSender("someone@somewhere");
        state.getRecipients().add("someoneelse@somewhere");
        state.setClientType(ClientType.EXTENDED);
        RsetHandler handler = new RsetHandler();
        SMTPReply reply = handler.handle(null, null, state);
        Assert.assertNotNull(reply);
        Assert.assertEquals(250, reply.getCode());
        Assert.assertEquals(new SMTPCode(2, 0, 0), reply.getEnhancedCode());
        Assert.assertNotNull(state.getClientType());
        Assert.assertNull(state.getSender());
        Assert.assertTrue(state.getRecipients().isEmpty());
        Assert.assertNull(state.getDataType());
    }

    @Test
    public void testNoopHandlerBasicResponse() throws Exception {
        ServerSessionState state = new ServerSessionState("whatever");
        state.setClientType(ClientType.BASIC);
        RsetHandler handler = new RsetHandler();
        SMTPReply reply = handler.handle(null, null, state);
        Assert.assertNotNull(reply);
        Assert.assertEquals(250, reply.getCode());
        Assert.assertNull(reply.getEnhancedCode());
    }

    @Test
    public void testNoopHandlerEnhancedResponse() throws Exception {
        ServerSessionState state = new ServerSessionState("whatever");
        state.setClientType(ClientType.EXTENDED);
        RsetHandler handler = new RsetHandler();
        SMTPReply reply = handler.handle(null, null, state);
        Assert.assertNotNull(reply);
        Assert.assertEquals(250, reply.getCode());
        Assert.assertEquals(new SMTPCode(2, 0, 0), reply.getEnhancedCode());
    }

    @Test
    public void testMailFromHandlerBasicResponse() throws Exception {
        ServerSessionState state = new ServerSessionState("whatever");
        state.setClientType(ClientType.BASIC);
        MailFromHandler handler = new MailFromHandler(null);
        SMTPReply reply = handler.handle("from:<someone@somedomain.com>", null, state);
        Assert.assertNotNull(reply);
        Assert.assertEquals(250, reply.getCode());
        Assert.assertNull(reply.getEnhancedCode());
        Assert.assertEquals("someone@somedomain.com", state.getSender());
    }

    @Test
    public void testMailFromHandlerEnhancedResponse() throws Exception {
        ServerSessionState state = new ServerSessionState("whatever");
        state.setClientType(ClientType.EXTENDED);
        MailFromHandler handler = new MailFromHandler(null);
        SMTPReply reply = handler.handle("from:<someone@somedomain.com>", null, state);
        Assert.assertNotNull(reply);
        Assert.assertEquals(250, reply.getCode());
        Assert.assertEquals(new SMTPCode(2, 1, 0), reply.getEnhancedCode());
        Assert.assertEquals("someone@somedomain.com", state.getSender());
    }

    @Test
    public void testMailFromHandlerClientTypeNotKnown() throws Exception {
        ServerSessionState state = new ServerSessionState("whatever");
        state.setClientType(null);
        MailFromHandler handler = new MailFromHandler(null);
        SMTPReply reply = handler.handle("from:<someone@somedomain.com>", null, state);
        Assert.assertNotNull(reply);
        Assert.assertEquals(503, reply.getCode());
        Assert.assertNull(reply.getEnhancedCode());
        Assert.assertNull(state.getSender());
    }

    @Test
    public void testMailFromHandlerSenderSet() throws Exception {
        ServerSessionState state = new ServerSessionState("whatever");
        state.setClientType(ClientType.BASIC);
        state.setSender("someone-else@somedomain.com");
        MailFromHandler handler = new MailFromHandler(null);
        SMTPReply reply = handler.handle("from:<someone@somedomain.com>", null, state);
        Assert.assertNotNull(reply);
        Assert.assertEquals(503, reply.getCode());
        Assert.assertNull(reply.getEnhancedCode());
        Assert.assertEquals("someone-else@somedomain.com", state.getSender());
    }
    @Test
    public void testMailFromHandlerSenderSetEnhancedResponse() throws Exception {
        ServerSessionState state = new ServerSessionState("whatever");
        state.setClientType(ClientType.EXTENDED);
        state.setSender("someone-else@somedomain.com");
        MailFromHandler handler = new MailFromHandler(null);
        SMTPReply reply = handler.handle("from:<someone@somedomain.com>", null, state);
        Assert.assertNotNull(reply);
        Assert.assertEquals(503, reply.getCode());
        Assert.assertEquals(new SMTPCode(5, 5, 1), reply.getEnhancedCode());
        Assert.assertEquals("someone-else@somedomain.com", state.getSender());
    }

    @Test
    public void testMailFromHandlerInvalidArgument() throws Exception {
        ServerSessionState state = new ServerSessionState("whatever");
        state.setClientType(ClientType.BASIC);
        MailFromHandler handler = new MailFromHandler(null);
        SMTPReply reply = handler.handle("from:me", null, state);
        Assert.assertNotNull(reply);
        Assert.assertEquals(500, reply.getCode());
        Assert.assertNull(reply.getEnhancedCode());
    }

    @Test
    public void testMailFromHandlerInvalidArgumentEnhancedResponse() throws Exception {
        ServerSessionState state = new ServerSessionState("whatever");
        state.setClientType(ClientType.EXTENDED);
        MailFromHandler handler = new MailFromHandler(null);
        SMTPReply reply = handler.handle("from:me", null, state);
        Assert.assertNotNull(reply);
        Assert.assertEquals(500, reply.getCode());
        Assert.assertEquals(new SMTPCode(5, 5, 1), reply.getEnhancedCode());
    }

    @Test
    public void testRcptToHandlerBasicResponse() throws Exception {
        ServerSessionState state = new ServerSessionState("whatever");
        state.setClientType(ClientType.BASIC);
        state.setSender("me@somedomain.com");
        RcptToHandler handler = new RcptToHandler(null);
        SMTPReply reply = handler.handle("to:<someone@somedomain.com>", null, state);
        Assert.assertNotNull(reply);
        Assert.assertEquals(250, reply.getCode());
        Assert.assertNull(reply.getEnhancedCode());
        Assert.assertEquals("me@somedomain.com", state.getSender());
        Assert.assertEquals(1, state.getRecipients().size());
        Assert.assertEquals("someone@somedomain.com", state.getRecipients().get(0));

        reply = handler.handle("to:<someone-else@somedomain.com>", null, state);
        Assert.assertNotNull(reply);
        Assert.assertEquals(250, reply.getCode());
        Assert.assertNull(reply.getEnhancedCode());
        Assert.assertEquals("me@somedomain.com", state.getSender());
        Assert.assertEquals(2, state.getRecipients().size());
        Assert.assertEquals("someone-else@somedomain.com", state.getRecipients().get(1));
    }

    @Test
    public void testRcptToHandlerEnhancedResponse() throws Exception {
        ServerSessionState state = new ServerSessionState("whatever");
        state.setClientType(ClientType.EXTENDED);
        state.setSender("me@somedomain.com");
        RcptToHandler handler = new RcptToHandler(null);
        SMTPReply reply = handler.handle("to:<someone@somedomain.com>", null, state);
        Assert.assertNotNull(reply);
        Assert.assertEquals(250, reply.getCode());
        Assert.assertEquals(new SMTPCode(2, 1, 5), reply.getEnhancedCode());
        Assert.assertEquals("me@somedomain.com", state.getSender());
        Assert.assertEquals(1, state.getRecipients().size());
        Assert.assertEquals("someone@somedomain.com", state.getRecipients().get(0));

        reply = handler.handle("to:<someone-else@somedomain.com>", null, state);
        Assert.assertNotNull(reply);
        Assert.assertEquals(new SMTPCode(2, 1, 5), reply.getEnhancedCode());
        Assert.assertEquals("me@somedomain.com", state.getSender());
        Assert.assertEquals(2, state.getRecipients().size());
        Assert.assertEquals("someone-else@somedomain.com", state.getRecipients().get(1));
    }

    @Test
    public void testRcptToHandlerClientTypeNotKnown() throws Exception {
        ServerSessionState state = new ServerSessionState("whatever");
        state.setClientType(null);
        RcptToHandler handler = new RcptToHandler(null);
        SMTPReply reply = handler.handle("to:<someone@somedomain.com>", null, state);
        Assert.assertNotNull(reply);
        Assert.assertEquals(503, reply.getCode());
        Assert.assertNull(reply.getEnhancedCode());
        Assert.assertNull(state.getSender());
    }

    @Test
    public void testRcptToHandlerSenderNotSet() throws Exception {
        ServerSessionState state = new ServerSessionState("whatever");
        state.setClientType(ClientType.BASIC);
        state.setSender(null);
        RcptToHandler handler = new RcptToHandler(null);
        SMTPReply reply = handler.handle("to:<someone@somedomain.com>", null, state);
        Assert.assertNotNull(reply);
        Assert.assertEquals(503, reply.getCode());
        Assert.assertNull(reply.getEnhancedCode());
    }

    @Test
    public void testRcptToHandlerSenderSetEnhancedResponse() throws Exception {
        ServerSessionState state = new ServerSessionState("whatever");
        state.setClientType(ClientType.EXTENDED);
        state.setSender(null);
        RcptToHandler handler = new RcptToHandler(null);
        SMTPReply reply = handler.handle("to:<someone@somedomain.com>", null, state);
        Assert.assertNotNull(reply);
        Assert.assertEquals(503, reply.getCode());
        Assert.assertEquals(new SMTPCode(5, 5, 1), reply.getEnhancedCode());
    }

    @Test
    public void testRcptToHandlerInvalidArgument() throws Exception {
        ServerSessionState state = new ServerSessionState("whatever");
        state.setClientType(ClientType.BASIC);
        state.setSender("me@somedomain.com");
        RcptToHandler handler = new RcptToHandler(null);
        SMTPReply reply = handler.handle("to:me", null, state);
        Assert.assertNotNull(reply);
        Assert.assertEquals(500, reply.getCode());
        Assert.assertNull(reply.getEnhancedCode());
    }

    @Test
    public void testRcptToHandlerInvalidArgumentEnhancedResponse() throws Exception {
        ServerSessionState state = new ServerSessionState("whatever");
        state.setClientType(ClientType.EXTENDED);
        state.setSender("me@somedomain.com");
        RcptToHandler handler = new RcptToHandler(null);
        SMTPReply reply = handler.handle("to:me", null, state);
        Assert.assertNotNull(reply);
        Assert.assertEquals(500, reply.getCode());
        Assert.assertEquals(new SMTPCode(5, 5, 1), reply.getEnhancedCode());
    }

    @Test
    public void testDataHandlerBasicResponse() throws Exception {
        ServerSessionState state = new ServerSessionState("whatever");
        state.setClientType(ClientType.BASIC);
        state.setSender("me@somedomain.com");
        state.getRecipients().add("someone@somedomain.com");
        DataHandler handler = new DataHandler();
        SMTPReply reply = handler.handle(null, null, state);
        Assert.assertNotNull(reply);
        Assert.assertEquals(354, reply.getCode());
        Assert.assertNull(reply.getEnhancedCode());
        Assert.assertEquals(DataType.ASCII, state.getDataType());
    }

    @Test
    public void testDataHandlerNoEnhancedResponse() throws Exception {
        ServerSessionState state = new ServerSessionState("whatever");
        state.setClientType(ClientType.EXTENDED);
        state.setSender("me@somedomain.com");
        state.getRecipients().add("someone@somedomain.com");
        DataHandler handler = new DataHandler();
        SMTPReply reply = handler.handle(null, null, state);
        Assert.assertNotNull(reply);
        Assert.assertEquals(354, reply.getCode());
        Assert.assertNull(reply.getEnhancedCode());
        Assert.assertEquals(DataType.ASCII, state.getDataType());
    }

    @Test
    public void testDataHandlerClientTypeNotKnown() throws Exception {
        ServerSessionState state = new ServerSessionState("whatever");
        state.setClientType(null);
        DataHandler handler = new DataHandler();
        SMTPReply reply = handler.handle(null, null, state);
        Assert.assertNotNull(reply);
        Assert.assertEquals(503, reply.getCode());
        Assert.assertNull(reply.getEnhancedCode());
        Assert.assertNull(state.getSender());
        Assert.assertNull(state.getDataType());
    }

    @Test
    public void testDataHandlerSenderNotSet() throws Exception {
        ServerSessionState state = new ServerSessionState("whatever");
        state.setClientType(ClientType.BASIC);
        state.setSender(null);
        DataHandler handler = new DataHandler();
        SMTPReply reply = handler.handle(null, null, state);
        Assert.assertNotNull(reply);
        Assert.assertEquals(503, reply.getCode());
        Assert.assertNull(reply.getEnhancedCode());
        Assert.assertNull(state.getDataType());
    }
    @Test
    public void testDataHandlerSenderSetEnhancedResponse() throws Exception {
        ServerSessionState state = new ServerSessionState("whatever");
        state.setClientType(ClientType.EXTENDED);
        state.setSender(null);
        DataHandler handler = new DataHandler();
        SMTPReply reply = handler.handle(null, null, state);
        Assert.assertNotNull(reply);
        Assert.assertEquals(503, reply.getCode());
        Assert.assertEquals(new SMTPCode(5, 5, 1), reply.getEnhancedCode());
        Assert.assertNull(state.getDataType());
    }

    @Test
    public void testDataHandlerNoRecipients() throws Exception {
        ServerSessionState state = new ServerSessionState("whatever");
        state.setClientType(ClientType.BASIC);
        state.setSender("me@somedomain.com");
        DataHandler handler = new DataHandler();
        SMTPReply reply = handler.handle(null, null, state);
        Assert.assertNotNull(reply);
        Assert.assertEquals(554, reply.getCode());
        Assert.assertNull(reply.getEnhancedCode());
        Assert.assertNull(state.getDataType());
    }

    @Test
    public void testDataHandlerNoRecipientsEnhancedResponse() throws Exception {
        ServerSessionState state = new ServerSessionState("whatever");
        state.setClientType(ClientType.EXTENDED);
        state.setSender("me@somedomain.com");
        DataHandler handler = new DataHandler();
        SMTPReply reply = handler.handle(null, null, state);
        Assert.assertNotNull(reply);
        Assert.assertEquals(554, reply.getCode());
        Assert.assertEquals(new SMTPCode(5, 5, 1), reply.getEnhancedCode());
        Assert.assertNull(state.getDataType());
    }

}
