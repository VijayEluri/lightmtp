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
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.SessionRequest;

import com.ok2c.lightmtp.agent.MailClientTransport;
import com.ok2c.lightmtp.agent.SessionEndpoint;
import com.ok2c.lightmtp.impl.agent.LocalMailClientTransport;
import com.ok2c.lightmtp.message.content.ByteArraySource;
import com.ok2c.lightmtp.protocol.BasicDeliveryRequest;
import com.ok2c.lightmtp.protocol.DeliveryRequest;
import com.ok2c.lightmtp.protocol.DeliveryRequestHandler;
import com.ok2c.lightmtp.protocol.DeliveryResult;
import com.ok2c.lightmtp.protocol.SessionContext;

public class LocalMailClientTransportExample {

    public static void main(final String[] args) throws Exception {

        String text1 = "From: root\r\n" +
                "To: testuser1\r\n" +
                "Subject: test message 1\r\n" +
                "\r\n" +
                "This is a short test message 1\r\n";
        String text2 = "From: root\r\n" +
                "To: testuser1, testuser2\r\n" +
                "Subject: test message 2\r\n" +
                "\r\n" +
                "This is a short test message 2\r\n";
        String text3 = "From: root\r\n" +
                "To: testuser1, testuser2, testuser3\r\n" +
                "Subject: test message 3\r\n" +
                "\r\n" +
                "This is a short test message 3\r\n";

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

        CountDownLatch messageCount = new CountDownLatch(queue.size());

        IOReactorConfig config = IOReactorConfig.custom()
                .setIoThreadCount(1)
                .build();

        MailClientTransport mua = new LocalMailClientTransport(config);
        mua.start(new MyDeliveryRequestHandler(messageCount));

        SessionEndpoint endpoint = new SessionEndpoint(new InetSocketAddress("localhost", 2525));

        SessionRequest sessionRequest = mua.connect(endpoint, queue, null);
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
        try {
            mua.shutdown();
        } catch (IOException ex) {
            mua.forceShutdown();
        }
        System.out.println("Done");
    }

    static class MyDeliveryRequestHandler implements DeliveryRequestHandler {

        private final CountDownLatch messageCount;

        public MyDeliveryRequestHandler(final CountDownLatch messageCount) {
            super();
            this.messageCount = messageCount;
        }

        @Override
        public void connected(final SessionContext context) {
            System.out.println("Connected");
        }

        @Override
        public void disconnected(final SessionContext context) {
            while (this.messageCount.getCount() > 0) {
                this.messageCount.countDown();
            }
            System.out.println("Disconnected");
        }

        @Override
        public void exception(final Exception ex, final SessionContext context) {
            while (this.messageCount.getCount() > 0) {
                this.messageCount.countDown();
            }
            System.out.println("Error: " + ex.getMessage());
        }

        @Override
        public void completed(
                final DeliveryRequest request,
                final DeliveryResult result,
                final SessionContext context) {
            this.messageCount.countDown();
            System.out.println("Message delivery succeeded: " + request + "; " + result);
        }

        @Override
        public void failed(
                final DeliveryRequest request,
                final DeliveryResult result,
                final SessionContext context) {
            this.messageCount.countDown();
            System.out.println("Message delivery failed: " + request + "; " + result);
        }

        @Override
        public DeliveryRequest submitRequest(final SessionContext context) {
            @SuppressWarnings("unchecked")
            Queue<DeliveryRequest> queue = (Queue<DeliveryRequest>) context.getAttribute(
                    IOSession.ATTACHMENT_KEY);
            return queue.poll();
        }

    }

}
