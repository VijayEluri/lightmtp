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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.ok2c.lightmtp.SMTPCode;
import com.ok2c.lightmtp.SMTPCodes;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.protocol.BasicDeliveryResult;
import com.ok2c.lightmtp.protocol.DeliveryHandler;
import com.ok2c.lightmtp.protocol.DeliveryRequest;
import com.ok2c.lightmtp.protocol.DeliveryResult;
import com.ok2c.lightnio.concurrent.BasicFuture;

public class DelayedTestDeliveryHandler implements DeliveryHandler {

    private final Queue<SimpleTestDelivery> deliveries;
    
    public DelayedTestDeliveryHandler() {
        super();
        this.deliveries = new ConcurrentLinkedQueue<SimpleTestDelivery>();
    }
    
    public Queue<SimpleTestDelivery> getDeliveries() {
        return this.deliveries;
    }

    public void handle(final DeliveryRequest request, final BasicFuture<DeliveryResult> future) {
        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    Thread.sleep(200);
                    SimpleTestDelivery delivery = new SimpleTestDelivery();
                    delivery.setSender(request.getSender());
                    delivery.setRecipients(request.getRecipients());
                    delivery.setContent(ContentUtils.readToString(request.getContent()));
                    deliveries.add(delivery);
                    SMTPReply reply = new SMTPReply(SMTPCodes.OK, new SMTPCode(2, 6, 0), 
                            "message accepted");
                    future.completed(new BasicDeliveryResult(reply));
                } catch (InterruptedException ex) {
                    future.failed(ex);
                } catch (IOException ex) {
                    future.failed(ex);
                }
            }
            
        };
        t.start();
    }

}
