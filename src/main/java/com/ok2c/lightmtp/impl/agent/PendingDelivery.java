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

import com.ok2c.lightmtp.protocol.DeliveryRequest;
import com.ok2c.lightmtp.protocol.DeliveryResult;
import com.ok2c.lightnio.concurrent.BasicFuture;
import com.ok2c.lightnio.pool.ManagedIOSession;

class PendingDelivery {

    private final DeliveryRequest request;
    private final BasicFuture<DeliveryResult> deliveryFuture;

    private ManagedIOSession managedSession;

    public PendingDelivery(
            final DeliveryRequest request,
            final BasicFuture<DeliveryResult> deliveryFuture) {
        super();
        this.request = request;
        this.deliveryFuture = deliveryFuture;
    }

    public DeliveryRequest getRequest() {
        return this.request;
    }

    public BasicFuture<DeliveryResult> getDeliveryFuture() {
        return this.deliveryFuture;
    }

    public ManagedIOSession getManagedSession() {
        return this.managedSession;
    }

    public void setManagedSession(final ManagedIOSession managedSession) {
        this.managedSession = managedSession;
    }

}
