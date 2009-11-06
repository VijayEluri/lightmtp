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
package com.ok2c.lightmtp.examples;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

import com.ok2c.lightmtp.agent.IOSessionRegistry;
import com.ok2c.lightmtp.impl.agent.ClientIOEventDispatch;
import com.ok2c.lightmtp.impl.protocol.ClientSessionFactory;
import com.ok2c.lightmtp.message.content.ByteArraySource;
import com.ok2c.lightmtp.protocol.BasicDeliveryRequest;
import com.ok2c.lightmtp.protocol.DeliveryRequest;
import com.ok2c.lightmtp.protocol.DeliveryRequestHandler;
import com.ok2c.lightmtp.protocol.DeliveryResult;
import com.ok2c.lightmtp.protocol.SessionContext;
import com.ok2c.lightnio.IOEventDispatch;
import com.ok2c.lightnio.IOSession;
import com.ok2c.lightnio.SessionRequest;
import com.ok2c.lightnio.impl.DefaultConnectingIOReactor;
import com.ok2c.lightnio.impl.ExceptionEvent;
import com.ok2c.lightnio.impl.IOReactorConfig;

public class SMTPClientRequestHandlerExample {

    public static void main(String[] args) throws Exception {

        String text1 = new String(
                "From: root\r\n" +
                "To: testuser1\r\n" +
                "Subject: test message 1\r\n" +
                "\r\n" +
                "This is a short test message 1\r\n");
        String text2 = new String(
                "From: root\r\n" +
                "To: testuser1, testuser2\r\n" +
                "Subject: test message 2\r\n" +
                "\r\n" +
                "This is a short test message 2\r\n");
        String text3 = new String(
                "From: root\r\n" +
                "To: testuser1, testuser2, testuser3\r\n" +
                "Subject: test message 3\r\n" +
                "\r\n" +
                "This is a short test message 3\r\n");
        
        DeliveryRequest request1 = new BasicDeliveryRequest(
                "root", 
                Arrays.asList("testuser1"), 
                new ByteArraySource(text1.getBytes("US-ASCII")));

        DeliveryRequest request2 = new BasicDeliveryRequest(
                "root", 
                Arrays.asList("testuser1", "testuser2"), 
                new ByteArraySource(text2.getBytes("US-ASCII")));
        
        DeliveryRequest request3 = new BasicDeliveryRequest(
                "root", 
                Arrays.asList("testuser1", "testuser2", "testuser3"), 
                new ByteArraySource(text3.getBytes("US-ASCII")));
        
        Queue<DeliveryRequest> queue = new ConcurrentLinkedQueue<DeliveryRequest>();
        queue.add(request1);
        queue.add(request2);
        queue.add(request3);
        
        final CountDownLatch messageCount = new CountDownLatch(queue.size());
        final MyDeliveryRequestHandler handler = new MyDeliveryRequestHandler(messageCount);
        final ClientSessionFactory sessionFactory = new ClientSessionFactory(handler);
        final IOSessionRegistry sessionRegistry = new IOSessionRegistry();
        final IOEventDispatch ioEventDispatch = new ClientIOEventDispatch(
                sessionRegistry, sessionFactory);
        final DefaultConnectingIOReactor ioReactor = new DefaultConnectingIOReactor(
                new IOReactorConfig());
        
        Thread t = new Thread(new Runnable() {
            
            public void run() {
                try {
                    ioReactor.execute(ioEventDispatch);
                } catch (InterruptedIOException ex) {
                    System.err.println("Interrupted");
                } catch (IOException e) {
                    System.err.println("I/O error: " + e.getMessage());
                }
                
                List<ExceptionEvent> events = ioReactor.getAuditLog();
                if (events != null) {
                    for (ExceptionEvent event: events) {
                        event.getCause().printStackTrace();
                    }
                }
                
                while (messageCount.getCount() > 0) {
                    messageCount.countDown();
                }
                System.out.println("Shutdown");
            }
            
        });
        t.start();
        
        SessionRequest sessionRequest = ioReactor.connect(
                new InetSocketAddress("192.168.44.128", 25), null, queue, null);
        
        sessionRequest.waitFor();
        
        IOSession iosession = sessionRequest.getSession();
        if (iosession != null) {
            messageCount.await();
        } else {
            IOException ex = sessionRequest.getException();
            if (ex != null) {
                System.out.println("Connection failed: " + ex.getMessage());
            }
        }
        
        System.out.println("Shutting down I/O reactor");
        ioReactor.shutdown();
        System.out.println("Done");
    }

    static class MyDeliveryRequestHandler implements DeliveryRequestHandler {

        private final CountDownLatch messageCount;
        
        public MyDeliveryRequestHandler(final CountDownLatch messageCount) {
            super();
            this.messageCount = messageCount;
        }

        public void connected(final SessionContext context) {
            System.out.println("Connected");
        }

        public void disconnected(final SessionContext context) {
            while (this.messageCount.getCount() > 0) {
                this.messageCount.countDown();
            }
            System.out.println("Disconnected");
        }

        public void exception(final Exception ex, final SessionContext context) {
            while (this.messageCount.getCount() > 0) {
                this.messageCount.countDown();
            }
            System.out.println("Error: " + ex.getMessage());
        }

        public void completed(
                final DeliveryRequest request, 
                final DeliveryResult result, 
                final SessionContext context) {
            System.out.println("Message delivery succeeded: " + request + "; " + result);
        }

        public void failed(
                final DeliveryRequest request, 
                final DeliveryResult result, 
                final SessionContext context) {
            System.out.println("Message delivery failed: " + request + "; " + result);
        }

        public DeliveryRequest submitRequest(final SessionContext context) {
            @SuppressWarnings("unchecked")
            Queue<DeliveryRequest> queue = (Queue<DeliveryRequest>) context.getAttribute(
                    IOSession.ATTACHMENT_KEY);
            return queue.poll();
        }
        
    }
    
}
