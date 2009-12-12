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
package com.ok2c.lightmtp.impl.agent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Future;

import junit.framework.Assert;

import org.junit.Test;

import com.ok2c.lightmtp.SMTPCode;
import com.ok2c.lightmtp.SMTPCodes;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.impl.BaseTransportTest;
import com.ok2c.lightmtp.impl.protocol.LocalClientSessionFactory;
import com.ok2c.lightmtp.impl.protocol.LocalServerSessionFactory;
import com.ok2c.lightmtp.impl.protocol.ServerSessionFactory;
import com.ok2c.lightmtp.impl.protocol.ServerState;
import com.ok2c.lightmtp.impl.protocol.cmd.DefaultProtocolHandler;
import com.ok2c.lightmtp.message.content.ByteArraySource;
import com.ok2c.lightmtp.protocol.BasicDeliveryRequest;
import com.ok2c.lightmtp.protocol.DeliveryHandler;
import com.ok2c.lightmtp.protocol.DeliveryRequest;
import com.ok2c.lightmtp.protocol.DeliveryResult;
import com.ok2c.lightmtp.protocol.EnvelopValidator;
import com.ok2c.lightmtp.protocol.ProtocolHandler;
import com.ok2c.lightmtp.protocol.RcptResult;
import com.ok2c.lightnio.IOReactorStatus;
import com.ok2c.lightnio.ListenerEndpoint;
import com.ok2c.lightnio.SessionRequest;
import com.ok2c.lightnio.concurrent.BasicFuture;
import com.ok2c.lightnio.concurrent.FutureCallback;

public class TestMailDelivery extends BaseTransportTest {

    static String TEXT1 = new String(
            "From: root\r\n" +
            "To: testuser1\r\n" +
            "Subject: test message 1\r\n" +
            "\r\n" +
            "This is a short test message 1\r\n");
    static String TEXT2 = new String(
            "From: root\r\n" +
            "To: testuser1, testuser2\r\n" +
            "Subject: test message 2\r\n" +
            "\r\n" +
            "This is a short test message 2\r\n");
    static String TEXT3 = new String(
            "From: root\r\n" +
            "To: testuser1, testuser2, testuser3\r\n" +
            "Subject: test message 3\r\n" +
            "\r\n" +
            "This is a short test message 3\r\n" +
            ". Period.\r\n");
    
    @Test
    public void testBasicPipelinedDelivery() throws Exception {
        
        List<DeliveryRequest> requests = new ArrayList<DeliveryRequest>();
        requests.add(new BasicDeliveryRequest(
                "root", 
                Arrays.asList("testuser1"), 
                new ByteArraySource(TEXT1.getBytes("US-ASCII"))));
        requests.add(new BasicDeliveryRequest(
                "root", 
                Arrays.asList("testuser1", "testuser2"), 
                new ByteArraySource(TEXT2.getBytes("US-ASCII"))));
        
        requests.add(new BasicDeliveryRequest(
                "root", 
                Arrays.asList("testuser1", "testuser2", "testuser3"), 
                new ByteArraySource(TEXT3.getBytes("US-ASCII"))));
        
        SimpleTestJob testJob = new SimpleTestJob(requests);

        SimpleTestDeliveryHandler deliveryHandler = new SimpleTestDeliveryHandler();
        this.mta.start(new SimpleEnvelopValidator(), deliveryHandler);
        ListenerEndpoint endpoint = this.mta.listen(new InetSocketAddress("localhost", 0));
        endpoint.waitFor();
        SocketAddress address = endpoint.getAddress();
        Assert.assertNotNull(address);
        Assert.assertNull(endpoint.getException());
        
        Assert.assertEquals(IOReactorStatus.ACTIVE, this.mta.getStatus());
        
        SimpleTestDeliveryRequestHandler deliveryRequestHandler = new SimpleTestDeliveryRequestHandler();
        this.mua.start(deliveryRequestHandler);
        
        SessionRequest sessionRequest = this.mua.connect(address, testJob, null);
        sessionRequest.waitFor();
        Assert.assertNotNull(sessionRequest.getSession());
        Assert.assertNull(sessionRequest.getException());
        
        List<DeliveryResult> results = testJob.waitForResults();
        Assert.assertNotNull(results);
        Assert.assertEquals(3, results.size());
        DeliveryResult res1 = results.get(0);
        Assert.assertTrue(res1.getFailures().isEmpty());
        Assert.assertEquals(250, res1.getReply().getCode());
        Assert.assertEquals(new SMTPCode(2, 6, 0), res1.getReply().getEnhancedCode());
        DeliveryResult res2 = results.get(1);
        Assert.assertTrue(res2.getFailures().isEmpty());
        Assert.assertEquals(250, res2.getReply().getCode());
        Assert.assertEquals(new SMTPCode(2, 6, 0), res2.getReply().getEnhancedCode());
        DeliveryResult res3 = results.get(2);
        Assert.assertTrue(res3.getFailures().isEmpty());
        Assert.assertEquals(250, res3.getReply().getCode());
        Assert.assertEquals(new SMTPCode(2, 6, 0), res3.getReply().getEnhancedCode());
        
        Queue<SimpleTestDelivery> deliveries = deliveryHandler.getDeliveries();
        Assert.assertNotNull(deliveries);
        SimpleTestDelivery delivery1 = deliveries.poll();
        Assert.assertNotNull(delivery1); 
        Assert.assertEquals("root", delivery1.getSender());
        Assert.assertEquals(1, delivery1.getRecipients().size());
        Assert.assertEquals("testuser1", delivery1.getRecipients().get(0));
        Assert.assertEquals(TEXT1, delivery1.getContent());
        SimpleTestDelivery delivery2 = deliveries.poll();
        Assert.assertNotNull(delivery2); 
        Assert.assertEquals("root", delivery2.getSender());
        Assert.assertEquals(2, delivery2.getRecipients().size());
        Assert.assertEquals("testuser1", delivery2.getRecipients().get(0));
        Assert.assertEquals("testuser2", delivery2.getRecipients().get(1));
        Assert.assertEquals(TEXT2, delivery2.getContent());
        SimpleTestDelivery delivery3 = deliveries.poll();
        Assert.assertNotNull(delivery3); 
        Assert.assertEquals("root", delivery3.getSender());
        Assert.assertEquals(3, delivery3.getRecipients().size());
        Assert.assertEquals("testuser1", delivery3.getRecipients().get(0));
        Assert.assertEquals("testuser2", delivery3.getRecipients().get(1));
        Assert.assertEquals("testuser3", delivery3.getRecipients().get(2));
        Assert.assertEquals(TEXT3, delivery3.getContent());
        Assert.assertNull(deliveries.poll()); 
        
    }
    
    @Test
    public void testDelayedDelivery() throws Exception {
        
        List<DeliveryRequest> requests = new ArrayList<DeliveryRequest>();
        requests.add(new BasicDeliveryRequest(
                "root", 
                Arrays.asList("testuser1"), 
                new ByteArraySource(TEXT1.getBytes("US-ASCII"))));
        requests.add(new BasicDeliveryRequest(
                "root", 
                Arrays.asList("testuser1", "testuser2"), 
                new ByteArraySource(TEXT2.getBytes("US-ASCII"))));
        
        requests.add(new BasicDeliveryRequest(
                "root", 
                Arrays.asList("testuser1", "testuser2", "testuser3"), 
                new ByteArraySource(TEXT3.getBytes("US-ASCII"))));
        
        SimpleTestJob testJob = new SimpleTestJob(requests);

        DelayedTestDeliveryHandler deliveryHandler = new DelayedTestDeliveryHandler();
        this.mta.start(new SimpleEnvelopValidator(), deliveryHandler);
        ListenerEndpoint endpoint = this.mta.listen(new InetSocketAddress("localhost", 0));
        endpoint.waitFor();
        SocketAddress address = endpoint.getAddress();
        Assert.assertNotNull(address);
        Assert.assertNull(endpoint.getException());
        
        Assert.assertEquals(IOReactorStatus.ACTIVE, this.mta.getStatus());
        
        SimpleTestDeliveryRequestHandler deliveryRequestHandler = new SimpleTestDeliveryRequestHandler();
        this.mua.start(deliveryRequestHandler);
        
        SessionRequest sessionRequest = this.mua.connect(address, testJob, null);
        sessionRequest.waitFor();
        Assert.assertNotNull(sessionRequest.getSession());
        Assert.assertNull(sessionRequest.getException());
        
        List<DeliveryResult> results = testJob.waitForResults();
        Assert.assertNotNull(results);
        Assert.assertEquals(3, results.size());
        DeliveryResult res1 = results.get(0);
        Assert.assertTrue(res1.getFailures().isEmpty());
        Assert.assertEquals(250, res1.getReply().getCode());
        Assert.assertEquals(new SMTPCode(2, 6, 0), res1.getReply().getEnhancedCode());
        DeliveryResult res2 = results.get(1);
        Assert.assertTrue(res2.getFailures().isEmpty());
        Assert.assertEquals(250, res2.getReply().getCode());
        Assert.assertEquals(new SMTPCode(2, 6, 0), res2.getReply().getEnhancedCode());
        DeliveryResult res3 = results.get(2);
        Assert.assertTrue(res3.getFailures().isEmpty());
        Assert.assertEquals(250, res3.getReply().getCode());
        Assert.assertEquals(new SMTPCode(2, 6, 0), res3.getReply().getEnhancedCode());
        
        Queue<SimpleTestDelivery> deliveries = deliveryHandler.getDeliveries();
        Assert.assertNotNull(deliveries);
        SimpleTestDelivery delivery1 = deliveries.poll();
        Assert.assertNotNull(delivery1); 
        Assert.assertEquals("root", delivery1.getSender());
        Assert.assertEquals(1, delivery1.getRecipients().size());
        Assert.assertEquals("testuser1", delivery1.getRecipients().get(0));
        Assert.assertEquals(TEXT1, delivery1.getContent());
        SimpleTestDelivery delivery2 = deliveries.poll();
        Assert.assertNotNull(delivery2); 
        Assert.assertEquals("root", delivery2.getSender());
        Assert.assertEquals(2, delivery2.getRecipients().size());
        Assert.assertEquals("testuser1", delivery2.getRecipients().get(0));
        Assert.assertEquals("testuser2", delivery2.getRecipients().get(1));
        Assert.assertEquals(TEXT2, delivery2.getContent());
        SimpleTestDelivery delivery3 = deliveries.poll();
        Assert.assertNotNull(delivery3); 
        Assert.assertEquals("root", delivery3.getSender());
        Assert.assertEquals(3, delivery3.getRecipients().size());
        Assert.assertEquals("testuser1", delivery3.getRecipients().get(0));
        Assert.assertEquals("testuser2", delivery3.getRecipients().get(1));
        Assert.assertEquals("testuser3", delivery3.getRecipients().get(2));
        Assert.assertEquals(TEXT3, delivery3.getContent());
        Assert.assertNull(deliveries.poll()); 
    }

    static class OldServerSessionFactory extends ServerSessionFactory {
        
        public OldServerSessionFactory(
                final EnvelopValidator validator,
                final DeliveryHandler deliveryHandler) {
            super(TMP_DIR, null, validator, deliveryHandler);
        }

        @Override
        protected ProtocolHandler<ServerState> createProtocolHandler(
                final EnvelopValidator validator) {
            DefaultProtocolHandler handler = (DefaultProtocolHandler) super.createProtocolHandler(
                    validator);
            handler.unregister("EHLO");
            return handler;
        }
        
    };
    
    @Test
    public void testBasicNonPipelinedDelivery() throws Exception {
        
        List<DeliveryRequest> requests = new ArrayList<DeliveryRequest>();
        requests.add(new BasicDeliveryRequest(
                "root", 
                Arrays.asList("testuser1"), 
                new ByteArraySource(TEXT1.getBytes("US-ASCII"))));
        requests.add(new BasicDeliveryRequest(
                "root", 
                Arrays.asList("testuser1", "testuser2"), 
                new ByteArraySource(TEXT2.getBytes("US-ASCII"))));
        
        requests.add(new BasicDeliveryRequest(
                "root", 
                Arrays.asList("testuser1", "testuser2", "testuser3"), 
                new ByteArraySource(TEXT3.getBytes("US-ASCII"))));
        
        SimpleTestJob testJob = new SimpleTestJob(requests);

        SimpleTestDeliveryHandler deliveryHandler = new SimpleTestDeliveryHandler();
        this.mta.start(new OldServerSessionFactory(new SimpleEnvelopValidator(), deliveryHandler));
        ListenerEndpoint endpoint = this.mta.listen(new InetSocketAddress("localhost", 0));
        endpoint.waitFor();
        SocketAddress address = endpoint.getAddress();
        Assert.assertNotNull(address);
        Assert.assertNull(endpoint.getException());
        
        Assert.assertEquals(IOReactorStatus.ACTIVE, this.mta.getStatus());
        
        SimpleTestDeliveryRequestHandler deliveryRequestHandler = new SimpleTestDeliveryRequestHandler();
        this.mua.start(deliveryRequestHandler);
        
        SessionRequest sessionRequest = this.mua.connect(address, testJob, null);
        sessionRequest.waitFor();
        Assert.assertNotNull(sessionRequest.getSession());
        Assert.assertNull(sessionRequest.getException());
        
        List<DeliveryResult> results = testJob.waitForResults();
        Assert.assertNotNull(results);
        Assert.assertEquals(3, results.size());
        DeliveryResult res1 = results.get(0);
        Assert.assertTrue(res1.getFailures().isEmpty());
        Assert.assertEquals(250, res1.getReply().getCode());
        Assert.assertNull(res1.getReply().getEnhancedCode());
        DeliveryResult res2 = results.get(1);
        Assert.assertTrue(res2.getFailures().isEmpty());
        Assert.assertEquals(250, res2.getReply().getCode());
        Assert.assertNull(res2.getReply().getEnhancedCode());
        DeliveryResult res3 = results.get(2);
        Assert.assertTrue(res3.getFailures().isEmpty());
        Assert.assertEquals(250, res3.getReply().getCode());
        Assert.assertNull(res3.getReply().getEnhancedCode());
        
        Queue<SimpleTestDelivery> deliveries = deliveryHandler.getDeliveries();
        Assert.assertNotNull(deliveries);
        SimpleTestDelivery delivery1 = deliveries.poll();
        Assert.assertNotNull(delivery1); 
        Assert.assertEquals("root", delivery1.getSender());
        Assert.assertEquals(1, delivery1.getRecipients().size());
        Assert.assertEquals("testuser1", delivery1.getRecipients().get(0));
        Assert.assertEquals(TEXT1, delivery1.getContent());
        SimpleTestDelivery delivery2 = deliveries.poll();
        Assert.assertNotNull(delivery2); 
        Assert.assertEquals("root", delivery2.getSender());
        Assert.assertEquals(2, delivery2.getRecipients().size());
        Assert.assertEquals("testuser1", delivery2.getRecipients().get(0));
        Assert.assertEquals("testuser2", delivery2.getRecipients().get(1));
        Assert.assertEquals(TEXT2, delivery2.getContent());
        SimpleTestDelivery delivery3 = deliveries.poll();
        Assert.assertNotNull(delivery3); 
        Assert.assertEquals("root", delivery3.getSender());
        Assert.assertEquals(3, delivery3.getRecipients().size());
        Assert.assertEquals("testuser1", delivery3.getRecipients().get(0));
        Assert.assertEquals("testuser2", delivery3.getRecipients().get(1));
        Assert.assertEquals("testuser3", delivery3.getRecipients().get(2));
        Assert.assertEquals(TEXT3, delivery3.getContent());
        Assert.assertNull(deliveries.poll()); 
        
    }

    @Test
    public void testRecipientRejectionPipelinedDelivery() throws Exception {
        
        List<DeliveryRequest> requests = new ArrayList<DeliveryRequest>();
        requests.add(new BasicDeliveryRequest(
                "root", 
                Arrays.asList("testuser1"), 
                new ByteArraySource(TEXT1.getBytes("US-ASCII"))));
        requests.add(new BasicDeliveryRequest(
                "root", 
                Arrays.asList("testuser1", "testuser2"), 
                new ByteArraySource(TEXT2.getBytes("US-ASCII"))));
        
        SimpleTestJob testJob = new SimpleTestJob(requests);

        EnvelopValidator envelopValidator = new SimpleEnvelopValidator() {

            @Override
            public Future<SMTPReply> validateRecipient(
                    final String recipient,
                    final FutureCallback<SMTPReply> callback) {
                if (recipient.equals("testuser1")) {
                    BasicFuture<SMTPReply> future = new BasicFuture<SMTPReply>(callback);
                    SMTPReply reply = new SMTPReply(SMTPCodes.ERR_PERM_MAILBOX_UNAVAILABLE,
                            new SMTPCode(5, 1, 1), 
                            "requested action not taken: mailbox unavailable");        
                    future.completed(reply);
                    return future;
                }
                return super.validateRecipient(recipient, callback);
            }

        };
        
        
        SimpleTestDeliveryHandler deliveryHandler = new SimpleTestDeliveryHandler();
        this.mta.start(envelopValidator, deliveryHandler);
        ListenerEndpoint endpoint = this.mta.listen(new InetSocketAddress("localhost", 0));
        endpoint.waitFor();
        SocketAddress address = endpoint.getAddress();
        Assert.assertNotNull(address);
        Assert.assertNull(endpoint.getException());
        
        Assert.assertEquals(IOReactorStatus.ACTIVE, this.mta.getStatus());
        
        SimpleTestDeliveryRequestHandler deliveryRequestHandler = new SimpleTestDeliveryRequestHandler();
        this.mua.start(deliveryRequestHandler);
        
        SessionRequest sessionRequest = this.mua.connect(address, testJob, null);
        sessionRequest.waitFor();
        Assert.assertNotNull(sessionRequest.getSession());
        Assert.assertNull(sessionRequest.getException());
        
        List<DeliveryResult> results = testJob.waitForResults();
        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.size());
        
        DeliveryResult res1 = results.get(0);
        Assert.assertEquals(1, res1.getFailures().size());
        RcptResult rcres1 = res1.getFailures().get(0);
        Assert.assertEquals("testuser1", rcres1.getRecipient());
        Assert.assertEquals(550, rcres1.getReply().getCode());
        Assert.assertEquals(new SMTPCode(5, 1, 1), rcres1.getReply().getEnhancedCode());
        Assert.assertEquals(503, res1.getReply().getCode());
        Assert.assertEquals(new SMTPCode(5, 5, 1), res1.getReply().getEnhancedCode());
        
        DeliveryResult res2 = results.get(1);
        Assert.assertEquals(1, res2.getFailures().size());
        RcptResult rcres2 = res2.getFailures().get(0);
        Assert.assertEquals("testuser1", rcres2.getRecipient());
        Assert.assertEquals(550, rcres2.getReply().getCode());
        Assert.assertEquals(new SMTPCode(5, 1, 1), rcres2.getReply().getEnhancedCode());
        Assert.assertEquals(250, res2.getReply().getCode());
        Assert.assertEquals(new SMTPCode(2, 6, 0), res2.getReply().getEnhancedCode());
        
        Queue<SimpleTestDelivery> deliveries = deliveryHandler.getDeliveries();
        Assert.assertNotNull(deliveries);
        SimpleTestDelivery delivery1 = deliveries.poll();
        Assert.assertNotNull(delivery1); 
        Assert.assertEquals("root", delivery1.getSender());
        Assert.assertEquals(1, delivery1.getRecipients().size());
        Assert.assertEquals("testuser2", delivery1.getRecipients().get(0));
        Assert.assertEquals(TEXT2, delivery1.getContent());
        Assert.assertNull(deliveries.poll()); 
    }
    
    @Test
    public void testRecipientRejectionNonPipelinedDelivery() throws Exception {
        
        List<DeliveryRequest> requests = new ArrayList<DeliveryRequest>();
        requests.add(new BasicDeliveryRequest(
                "root", 
                Arrays.asList("testuser1"), 
                new ByteArraySource(TEXT1.getBytes("US-ASCII"))));
        requests.add(new BasicDeliveryRequest(
                "root", 
                Arrays.asList("testuser1", "testuser2"), 
                new ByteArraySource(TEXT2.getBytes("US-ASCII"))));
        
        SimpleTestJob testJob = new SimpleTestJob(requests);

        EnvelopValidator envelopValidator = new SimpleEnvelopValidator() {

            @Override
            public Future<SMTPReply> validateRecipient(
                    final String recipient,
                    final FutureCallback<SMTPReply> callback) {
                if (recipient.equals("testuser1")) {
                    BasicFuture<SMTPReply> future = new BasicFuture<SMTPReply>(callback);
                    SMTPReply reply = new SMTPReply(SMTPCodes.ERR_PERM_MAILBOX_UNAVAILABLE,
                            new SMTPCode(5, 1, 1), 
                            "requested action not taken: mailbox unavailable");        
                    future.completed(reply);
                    return future;
                }
                return super.validateRecipient(recipient, callback);
            }

        };
        
        
        SimpleTestDeliveryHandler deliveryHandler = new SimpleTestDeliveryHandler();
        this.mta.start(new OldServerSessionFactory(envelopValidator, deliveryHandler));
        ListenerEndpoint endpoint = this.mta.listen(new InetSocketAddress("localhost", 0));
        endpoint.waitFor();
        SocketAddress address = endpoint.getAddress();
        Assert.assertNotNull(address);
        Assert.assertNull(endpoint.getException());
        
        Assert.assertEquals(IOReactorStatus.ACTIVE, this.mta.getStatus());
        
        SimpleTestDeliveryRequestHandler deliveryRequestHandler = new SimpleTestDeliveryRequestHandler();
        this.mua.start(deliveryRequestHandler);
        
        SessionRequest sessionRequest = this.mua.connect(address, testJob, null);
        sessionRequest.waitFor();
        Assert.assertNotNull(sessionRequest.getSession());
        Assert.assertNull(sessionRequest.getException());
        
        List<DeliveryResult> results = testJob.waitForResults();
        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.size());
        
        DeliveryResult res1 = results.get(0);
        Assert.assertEquals(1, res1.getFailures().size());
        RcptResult rcres1 = res1.getFailures().get(0);
        Assert.assertEquals("testuser1", rcres1.getRecipient());
        Assert.assertEquals(550, rcres1.getReply().getCode());
        Assert.assertNull(rcres1.getReply().getEnhancedCode());
        Assert.assertEquals(550, res1.getReply().getCode());
        Assert.assertNull(res1.getReply().getEnhancedCode());
        
        DeliveryResult res2 = results.get(1);
        Assert.assertEquals(1, res2.getFailures().size());
        RcptResult rcres2 = res2.getFailures().get(0);
        Assert.assertEquals("testuser1", rcres2.getRecipient());
        Assert.assertEquals(550, rcres2.getReply().getCode());
        Assert.assertNull(rcres2.getReply().getEnhancedCode());
        Assert.assertEquals(250, res2.getReply().getCode());
        Assert.assertNull(res2.getReply().getEnhancedCode());
        
        Queue<SimpleTestDelivery> deliveries = deliveryHandler.getDeliveries();
        Assert.assertNotNull(deliveries);
        SimpleTestDelivery delivery1 = deliveries.poll();
        Assert.assertNotNull(delivery1); 
        Assert.assertEquals("root", delivery1.getSender());
        Assert.assertEquals(1, delivery1.getRecipients().size());
        Assert.assertEquals("testuser2", delivery1.getRecipients().get(0));
        Assert.assertEquals(TEXT2, delivery1.getContent());
        Assert.assertNull(deliveries.poll()); 
    }

    @Test
    public void testSenderRejectionPipelinedDelivery() throws Exception {
        
        List<DeliveryRequest> requests = new ArrayList<DeliveryRequest>();
        requests.add(new BasicDeliveryRequest(
                "root@somewhere.com", 
                Arrays.asList("testuser1"), 
                new ByteArraySource(TEXT1.getBytes("US-ASCII"))));
        requests.add(new BasicDeliveryRequest(
                "root@somewhere.com", 
                Arrays.asList("testuser1", "testuser2"), 
                new ByteArraySource(TEXT2.getBytes("US-ASCII"))));
        
        SimpleTestJob testJob = new SimpleTestJob(requests);

        EnvelopValidator envelopValidator = new SimpleEnvelopValidator() {

            @Override
            public Future<SMTPReply> validateSender(
                    final String sender,
                    final FutureCallback<SMTPReply> callback) {
                if (sender.equals("root@somewhere.com")) {
                    BasicFuture<SMTPReply> future = new BasicFuture<SMTPReply>(callback);
                    SMTPReply reply = new SMTPReply(SMTPCodes.ERR_PERM_MAILBOX_NOT_ALLOWED,
                            new SMTPCode(5, 1, 8), 
                            "bad sender's system address");        
                    future.completed(reply);
                    return future;
                }
                return super.validateSender(sender, callback);
            }

        };
        
        SimpleTestDeliveryHandler deliveryHandler = new SimpleTestDeliveryHandler();
        this.mta.start(envelopValidator, deliveryHandler);
        ListenerEndpoint endpoint = this.mta.listen(new InetSocketAddress("localhost", 0));
        endpoint.waitFor();
        SocketAddress address = endpoint.getAddress();
        Assert.assertNotNull(address);
        Assert.assertNull(endpoint.getException());
        
        Assert.assertEquals(IOReactorStatus.ACTIVE, this.mta.getStatus());
        
        SimpleTestDeliveryRequestHandler deliveryRequestHandler = new SimpleTestDeliveryRequestHandler();
        this.mua.start(deliveryRequestHandler);
        
        SessionRequest sessionRequest = this.mua.connect(address, testJob, null);
        sessionRequest.waitFor();
        Assert.assertNotNull(sessionRequest.getSession());
        Assert.assertNull(sessionRequest.getException());
        
        List<DeliveryResult> results = testJob.waitForResults();
        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.size());
        
        DeliveryResult res1 = results.get(0);
        Assert.assertEquals(1, res1.getFailures().size());
        
        RcptResult rcres1 = res1.getFailures().get(0);
        Assert.assertEquals("testuser1", rcres1.getRecipient());
        Assert.assertEquals(503, rcres1.getReply().getCode());
        
        Assert.assertEquals(503, res1.getReply().getCode());
        Assert.assertEquals(new SMTPCode(5, 5, 1), res1.getReply().getEnhancedCode());
        
        DeliveryResult res2 = results.get(1);
        Assert.assertEquals(2, res2.getFailures().size());
        RcptResult rcres2 = res2.getFailures().get(0);
        Assert.assertEquals("testuser1", rcres2.getRecipient());
        Assert.assertEquals(503, rcres2.getReply().getCode());
        RcptResult rcres3 = res2.getFailures().get(1);
        Assert.assertEquals("testuser2", rcres3.getRecipient());
        Assert.assertEquals(503, rcres3.getReply().getCode());
        Assert.assertEquals(503, res2.getReply().getCode());
        Assert.assertEquals(new SMTPCode(5, 5, 1), res2.getReply().getEnhancedCode());
        
        Queue<SimpleTestDelivery> deliveries = deliveryHandler.getDeliveries();
        Assert.assertNull(deliveries.poll()); 
    }
    
    @Test
    public void testSenderRejectionNonPipelinedDelivery() throws Exception {
        
        List<DeliveryRequest> requests = new ArrayList<DeliveryRequest>();
        requests.add(new BasicDeliveryRequest(
                "root@somewhere.com", 
                Arrays.asList("testuser1"), 
                new ByteArraySource(TEXT1.getBytes("US-ASCII"))));
        requests.add(new BasicDeliveryRequest(
                "root@somewhere.com", 
                Arrays.asList("testuser1", "testuser2"), 
                new ByteArraySource(TEXT2.getBytes("US-ASCII"))));
        
        SimpleTestJob testJob = new SimpleTestJob(requests);

        EnvelopValidator envelopValidator = new SimpleEnvelopValidator() {

            @Override
            public Future<SMTPReply> validateSender(
                    final String sender,
                    final FutureCallback<SMTPReply> callback) {
                if (sender.equals("root@somewhere.com")) {
                    BasicFuture<SMTPReply> future = new BasicFuture<SMTPReply>(callback);
                    SMTPReply reply = new SMTPReply(SMTPCodes.ERR_PERM_MAILBOX_NOT_ALLOWED,
                            new SMTPCode(5, 1, 8), 
                            "bad sender's system address");        
                    future.completed(reply);
                    return future;
                }
                return super.validateSender(sender, callback);
            }
            
        };
        
        SimpleTestDeliveryHandler deliveryHandler = new SimpleTestDeliveryHandler();
        this.mta.start(new OldServerSessionFactory(envelopValidator, deliveryHandler));
        ListenerEndpoint endpoint = this.mta.listen(new InetSocketAddress("localhost", 0));
        endpoint.waitFor();
        SocketAddress address = endpoint.getAddress();
        Assert.assertNotNull(address);
        Assert.assertNull(endpoint.getException());
        
        Assert.assertEquals(IOReactorStatus.ACTIVE, this.mta.getStatus());
        
        SimpleTestDeliveryRequestHandler deliveryRequestHandler = new SimpleTestDeliveryRequestHandler();
        this.mua.start(deliveryRequestHandler);
        
        SessionRequest sessionRequest = this.mua.connect(address, testJob, null);
        sessionRequest.waitFor();
        Assert.assertNotNull(sessionRequest.getSession());
        Assert.assertNull(sessionRequest.getException());
        
        List<DeliveryResult> results = testJob.waitForResults();
        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.size());
        
        DeliveryResult res1 = results.get(0);
        Assert.assertTrue(res1.getFailures().isEmpty());
        
        DeliveryResult res2 = results.get(1);
        Assert.assertTrue(res2.getFailures().isEmpty());
        
        Queue<SimpleTestDelivery> deliveries = deliveryHandler.getDeliveries();
        Assert.assertNull(deliveries.poll()); 
    }

    @Test
    public void testDelayedValidationPipelinedDelivery() throws Exception {
        
        List<DeliveryRequest> requests = new ArrayList<DeliveryRequest>();
        requests.add(new BasicDeliveryRequest(
                "root@somewhere.com", 
                Arrays.asList("testuser1"), 
                new ByteArraySource(TEXT1.getBytes("US-ASCII"))));
        requests.add(new BasicDeliveryRequest(
                "root@somewhere.com", 
                Arrays.asList("testuser1", "testuser2"), 
                new ByteArraySource(TEXT2.getBytes("US-ASCII"))));
        
        SimpleTestJob testJob = new SimpleTestJob(requests);

        SimpleTestDeliveryHandler deliveryHandler = new SimpleTestDeliveryHandler();
        this.mta.start(new DelayedEnvelopValidator(), deliveryHandler);
        ListenerEndpoint endpoint = this.mta.listen(new InetSocketAddress("localhost", 0));
        endpoint.waitFor();
        SocketAddress address = endpoint.getAddress();
        Assert.assertNotNull(address);
        Assert.assertNull(endpoint.getException());
        
        Assert.assertEquals(IOReactorStatus.ACTIVE, this.mta.getStatus());
        
        SimpleTestDeliveryRequestHandler deliveryRequestHandler = new SimpleTestDeliveryRequestHandler();
        this.mua.start(deliveryRequestHandler);
        
        SessionRequest sessionRequest = this.mua.connect(address, testJob, null);
        sessionRequest.waitFor();
        Assert.assertNotNull(sessionRequest.getSession());
        Assert.assertNull(sessionRequest.getException());
        
        List<DeliveryResult> results = testJob.waitForResults();
        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.size());
        
        DeliveryResult res1 = results.get(0);
        Assert.assertTrue(res1.getFailures().isEmpty());
        Assert.assertEquals(250, res1.getReply().getCode());
        Assert.assertEquals(new SMTPCode(2, 6, 0), res1.getReply().getEnhancedCode());
        
        DeliveryResult res2 = results.get(1);
        Assert.assertTrue(res2.getFailures().isEmpty());
        Assert.assertEquals(250, res2.getReply().getCode());
        Assert.assertEquals(new SMTPCode(2, 6, 0), res2.getReply().getEnhancedCode());
    }

    @Test
    public void testDelayedValidationNonPipelinedDelivery() throws Exception {
        
        List<DeliveryRequest> requests = new ArrayList<DeliveryRequest>();
        requests.add(new BasicDeliveryRequest(
                "root@somewhere.com", 
                Arrays.asList("testuser1"), 
                new ByteArraySource(TEXT1.getBytes("US-ASCII"))));
        requests.add(new BasicDeliveryRequest(
                "root@somewhere.com", 
                Arrays.asList("testuser1", "testuser2"), 
                new ByteArraySource(TEXT2.getBytes("US-ASCII"))));
        
        SimpleTestJob testJob = new SimpleTestJob(requests);

        SimpleTestDeliveryHandler deliveryHandler = new SimpleTestDeliveryHandler();
        this.mta.start(new OldServerSessionFactory(new DelayedEnvelopValidator(), deliveryHandler));
        ListenerEndpoint endpoint = this.mta.listen(new InetSocketAddress("localhost", 0));
        endpoint.waitFor();
        SocketAddress address = endpoint.getAddress();
        Assert.assertNotNull(address);
        Assert.assertNull(endpoint.getException());
        
        Assert.assertEquals(IOReactorStatus.ACTIVE, this.mta.getStatus());
        
        SimpleTestDeliveryRequestHandler deliveryRequestHandler = new SimpleTestDeliveryRequestHandler();
        this.mua.start(deliveryRequestHandler);
        
        SessionRequest sessionRequest = this.mua.connect(address, testJob, null);
        sessionRequest.waitFor();
        Assert.assertNotNull(sessionRequest.getSession());
        Assert.assertNull(sessionRequest.getException());
        
        List<DeliveryResult> results = testJob.waitForResults();
        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.size());
        
        DeliveryResult res1 = results.get(0);
        Assert.assertTrue(res1.getFailures().isEmpty());
        Assert.assertEquals(250, res1.getReply().getCode());
        Assert.assertNull(res1.getReply().getEnhancedCode());
        
        DeliveryResult res2 = results.get(1);
        Assert.assertTrue(res2.getFailures().isEmpty());
        Assert.assertEquals(250, res2.getReply().getCode());
        Assert.assertNull(res2.getReply().getEnhancedCode());
    }

    @Test
    public void testDeliveryFailure() throws Exception {
        
        List<DeliveryRequest> requests = new ArrayList<DeliveryRequest>();
        requests.add(new BasicDeliveryRequest(
                "root@somewhere.com", 
                Arrays.asList("testuser1"), 
                new ByteArraySource(TEXT1.getBytes("US-ASCII"))));
        requests.add(new BasicDeliveryRequest(
                "root@somewhere.com", 
                Arrays.asList("testuser1", "testuser2"), 
                new ByteArraySource(TEXT2.getBytes("US-ASCII"))));
        
        SimpleTestJob testJob = new SimpleTestJob(requests);

        DeliveryHandler deliveryHandler = new DeliveryHandler() {

            public Future<DeliveryResult> handle(
                    final DeliveryRequest request, 
                    final FutureCallback<DeliveryResult> callback) {
                BasicFuture<DeliveryResult> future = new BasicFuture<DeliveryResult>(callback);
                future.failed(new IOException("Oooopsie"));
                return future;
            }
            
        };
        
        this.mta.start(new SimpleEnvelopValidator(), deliveryHandler);
        ListenerEndpoint endpoint = this.mta.listen(new InetSocketAddress("localhost", 0));
        endpoint.waitFor();
        SocketAddress address = endpoint.getAddress();
        Assert.assertNotNull(address);
        Assert.assertNull(endpoint.getException());
        
        Assert.assertEquals(IOReactorStatus.ACTIVE, this.mta.getStatus());
        
        SimpleTestDeliveryRequestHandler deliveryRequestHandler = new SimpleTestDeliveryRequestHandler();
        this.mua.start(deliveryRequestHandler);
        
        SessionRequest sessionRequest = this.mua.connect(address, testJob, null);
        sessionRequest.waitFor();
        Assert.assertNotNull(sessionRequest.getSession());
        Assert.assertNull(sessionRequest.getException());
        
        List<DeliveryResult> results = testJob.waitForResults();
        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.size());
        
        DeliveryResult res1 = results.get(0);
        Assert.assertTrue(res1.getFailures().isEmpty());
        Assert.assertEquals(451, res1.getReply().getCode());
        Assert.assertEquals(new SMTPCode(4, 2, 0), res1.getReply().getEnhancedCode());
        
        DeliveryResult res2 = results.get(1);
        Assert.assertTrue(res2.getFailures().isEmpty());
        Assert.assertEquals(451, res2.getReply().getCode());
        Assert.assertEquals(new SMTPCode(4, 2, 0), res2.getReply().getEnhancedCode());
    }

    @Test
    public void testBasicLocalDelivery() throws Exception {
        
        List<DeliveryRequest> requests = new ArrayList<DeliveryRequest>();
        requests.add(new BasicDeliveryRequest(
                "root", 
                Arrays.asList("testuser1"), 
                new ByteArraySource(TEXT1.getBytes("US-ASCII"))));
        requests.add(new BasicDeliveryRequest(
                "root", 
                Arrays.asList("testuser1", "testuser2"), 
                new ByteArraySource(TEXT2.getBytes("US-ASCII"))));
        
        requests.add(new BasicDeliveryRequest(
                "root", 
                Arrays.asList("testuser1", "testuser2", "testuser3"), 
                new ByteArraySource(TEXT3.getBytes("US-ASCII"))));
        
        SimpleTestJob testJob = new SimpleTestJob(requests);

        SimpleTestDeliveryHandler deliveryHandler = new SimpleTestDeliveryHandler();
        this.mta.start(new LocalServerSessionFactory(
                TMP_DIR, null, new SimpleEnvelopValidator(), deliveryHandler));
        ListenerEndpoint endpoint = this.mta.listen(new InetSocketAddress("localhost", 0));
        endpoint.waitFor();
        SocketAddress address = endpoint.getAddress();
        Assert.assertNotNull(address);
        Assert.assertNull(endpoint.getException());
        
        Assert.assertEquals(IOReactorStatus.ACTIVE, this.mta.getStatus());
        
        SimpleTestDeliveryRequestHandler deliveryRequestHandler = new SimpleTestDeliveryRequestHandler();
        this.mua.start(new LocalClientSessionFactory(deliveryRequestHandler));
        
        SessionRequest sessionRequest = this.mua.connect(address, testJob, null);
        sessionRequest.waitFor();
        Assert.assertNotNull(sessionRequest.getSession());
        Assert.assertNull(sessionRequest.getException());
        
        List<DeliveryResult> results = testJob.waitForResults();
        Assert.assertNotNull(results);
        Assert.assertEquals(3, results.size());
        DeliveryResult res1 = results.get(0);
        Assert.assertTrue(res1.getFailures().isEmpty());
        Assert.assertEquals(250, res1.getReply().getCode());
        Assert.assertEquals(new SMTPCode(2, 6, 0), res1.getReply().getEnhancedCode());
        DeliveryResult res2 = results.get(1);
        Assert.assertTrue(res2.getFailures().isEmpty());
        Assert.assertEquals(250, res2.getReply().getCode());
        Assert.assertEquals(new SMTPCode(2, 6, 0), res2.getReply().getEnhancedCode());
        DeliveryResult res3 = results.get(2);
        Assert.assertTrue(res3.getFailures().isEmpty());
        Assert.assertEquals(250, res3.getReply().getCode());
        Assert.assertEquals(new SMTPCode(2, 6, 0), res3.getReply().getEnhancedCode());
        
        Queue<SimpleTestDelivery> deliveries = deliveryHandler.getDeliveries();
        Assert.assertNotNull(deliveries);
        SimpleTestDelivery delivery1 = deliveries.poll();
        Assert.assertNotNull(delivery1); 
        Assert.assertEquals("root", delivery1.getSender());
        Assert.assertEquals(1, delivery1.getRecipients().size());
        Assert.assertEquals("testuser1", delivery1.getRecipients().get(0));
        Assert.assertEquals(TEXT1, delivery1.getContent());
        SimpleTestDelivery delivery2 = deliveries.poll();
        Assert.assertNotNull(delivery2); 
        Assert.assertEquals("root", delivery2.getSender());
        Assert.assertEquals(2, delivery2.getRecipients().size());
        Assert.assertEquals("testuser1", delivery2.getRecipients().get(0));
        Assert.assertEquals("testuser2", delivery2.getRecipients().get(1));
        Assert.assertEquals(TEXT2, delivery2.getContent());
        SimpleTestDelivery delivery3 = deliveries.poll();
        Assert.assertNotNull(delivery3); 
        Assert.assertEquals("root", delivery3.getSender());
        Assert.assertEquals(3, delivery3.getRecipients().size());
        Assert.assertEquals("testuser1", delivery3.getRecipients().get(0));
        Assert.assertEquals("testuser2", delivery3.getRecipients().get(1));
        Assert.assertEquals("testuser3", delivery3.getRecipients().get(2));
        Assert.assertEquals(TEXT3, delivery3.getContent());
        Assert.assertNull(deliveries.poll()); 
    }

    @Test
    public void testLocalDeliveryFailure() throws Exception {
        
        List<DeliveryRequest> requests = new ArrayList<DeliveryRequest>();
        requests.add(new BasicDeliveryRequest(
                "root", 
                Arrays.asList("testuser1", "testuser2", "testuser3"), 
                new ByteArraySource(TEXT3.getBytes("US-ASCII"))));
        
        SimpleTestJob testJob = new SimpleTestJob(requests);

        DeliveryHandler deliveryHandler = new DeliveryHandler() {

            public Future<DeliveryResult> handle(
                    final DeliveryRequest request, 
                    final FutureCallback<DeliveryResult> callback) {
                BasicFuture<DeliveryResult> future = new BasicFuture<DeliveryResult>(callback);
                future.failed(new IOException("Oooopsie"));
                return future;
            }
            
        };
        
        this.mta.start(new LocalServerSessionFactory(
                TMP_DIR, null, new SimpleEnvelopValidator(), deliveryHandler));
        ListenerEndpoint endpoint = this.mta.listen(new InetSocketAddress("localhost", 0));
        endpoint.waitFor();
        SocketAddress address = endpoint.getAddress();
        Assert.assertNotNull(address);
        Assert.assertNull(endpoint.getException());
        
        Assert.assertEquals(IOReactorStatus.ACTIVE, this.mta.getStatus());
        
        SimpleTestDeliveryRequestHandler deliveryRequestHandler = new SimpleTestDeliveryRequestHandler();
        this.mua.start(new LocalClientSessionFactory(deliveryRequestHandler));
        
        SessionRequest sessionRequest = this.mua.connect(address, testJob, null);
        sessionRequest.waitFor();
        Assert.assertNotNull(sessionRequest.getSession());
        Assert.assertNull(sessionRequest.getException());
        
        List<DeliveryResult> results = testJob.waitForResults();
        Assert.assertNotNull(results);
        Assert.assertEquals(1, results.size());
        
        DeliveryResult res1 = results.get(0);
        Assert.assertEquals(3, res1.getFailures().size());
        
        RcptResult rcres1 = res1.getFailures().get(0);
        Assert.assertEquals("testuser1", rcres1.getRecipient());
        Assert.assertEquals(451, rcres1.getReply().getCode());
        Assert.assertEquals(new SMTPCode(4, 2, 0), rcres1.getReply().getEnhancedCode());
        RcptResult rcres2 = res1.getFailures().get(1);
        Assert.assertEquals("testuser2", rcres2.getRecipient());
        Assert.assertEquals(451, rcres2.getReply().getCode());
        Assert.assertEquals(new SMTPCode(4, 2, 0), rcres2.getReply().getEnhancedCode());
        RcptResult rcres3 = res1.getFailures().get(2);
        Assert.assertEquals("testuser3", rcres3.getRecipient());
        Assert.assertEquals(451, rcres3.getReply().getCode());
        Assert.assertEquals(new SMTPCode(4, 2, 0), rcres3.getReply().getEnhancedCode());
        
        Assert.assertEquals(451, res1.getReply().getCode());
        Assert.assertEquals(new SMTPCode(4, 2, 0), res1.getReply().getEnhancedCode());
    }

}
