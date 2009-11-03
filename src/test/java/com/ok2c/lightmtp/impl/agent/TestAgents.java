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

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

import junit.framework.Assert;

import org.junit.Test;

import com.ok2c.lightmtp.impl.BaseTransportTest;
import com.ok2c.lightmtp.message.content.ByteArraySource;
import com.ok2c.lightmtp.protocol.BasicDeliveryRequest;
import com.ok2c.lightmtp.protocol.DeliveryRequest;
import com.ok2c.lightmtp.protocol.DeliveryResult;
import com.ok2c.lightnio.IOReactorStatus;
import com.ok2c.lightnio.ListenerEndpoint;
import com.ok2c.lightnio.SessionRequest;

public class TestAgents extends BaseTransportTest {

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
            "This is a short test message 3\r\n");
    
    @Test
    public void testBasicDelivery() throws Exception {
        
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
        this.mta.start(null, deliveryHandler);
        ListenerEndpoint endpoint = this.mta.listen(new InetSocketAddress("localhost", 0));
        endpoint.waitFor();
        SocketAddress address = endpoint.getAddress();
        Assert.assertNotNull(address);
        Assert.assertNull(endpoint.getException());
        
        Assert.assertEquals(IOReactorStatus.ACTIVE, this.mta.getStatus());
        
        SimpleTestDeliveryRequestHandler deliveryRequestHandler = new SimpleTestDeliveryRequestHandler();
        this.mua.start(deliveryRequestHandler);
        
        SessionRequest sessionRequest = this.mua.connect(address, null, testJob, null);
        sessionRequest.waitFor();
        Assert.assertNotNull(sessionRequest.getSession());
        Assert.assertNull(sessionRequest.getException());
        
        List<DeliveryResult> results = testJob.waitForResults();
        Assert.assertNotNull(results);
        
        Queue<SimpleTestDelivery> deliveries = deliveryHandler.getDeliveries();
        Assert.assertNotNull(deliveries);
    }

}
