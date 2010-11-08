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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Future;

import com.ok2c.lightmtp.agent.TransportType;
import com.ok2c.lightmtp.impl.agent.DefaultMailUserAgent;
import com.ok2c.lightmtp.message.content.ByteArraySource;
import com.ok2c.lightmtp.protocol.BasicDeliveryRequest;
import com.ok2c.lightmtp.protocol.DeliveryRequest;
import com.ok2c.lightmtp.protocol.DeliveryResult;
import com.ok2c.lightnio.impl.IOReactorConfig;

public class MailUserAgentExample {

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

        List<DeliveryRequest> requests = new ArrayList<DeliveryRequest>();
        requests.add(new BasicDeliveryRequest(
                "root",
                Arrays.asList("testuser1"),
                new ByteArraySource(text1.getBytes("US-ASCII"))));
        requests.add(new BasicDeliveryRequest(
                "root",
                Arrays.asList("testuser1", "testuser2"),
                new ByteArraySource(text2.getBytes("US-ASCII"))));
        requests.add(new BasicDeliveryRequest(
                "root",
                Arrays.asList("testuser1", "testuser2", "testuser3"),
                new ByteArraySource(text3.getBytes("US-ASCII"))));


        DefaultMailUserAgent mua = new DefaultMailUserAgent(TransportType.SMTP, new IOReactorConfig());
        mua.start();

        try {

            InetSocketAddress address = new InetSocketAddress("localhost", 2525);

            Queue<Future<DeliveryResult>> queue = new LinkedList<Future<DeliveryResult>>();
            for (DeliveryRequest request: requests) {
                queue.add(mua.deliver(address, request, null));
            }

            while (!queue.isEmpty()) {
                Future<DeliveryResult> future = queue.remove();
                DeliveryResult result = future.get();
                System.out.println("Delivery result: " + result);
            }

        } finally {
            mua.shutdown();
        }
    }

}
