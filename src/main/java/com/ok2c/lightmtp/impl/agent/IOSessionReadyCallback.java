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

import java.nio.channels.SelectionKey;

import com.ok2c.lightnio.IOSession;
import com.ok2c.lightnio.concurrent.FutureCallback;
import com.ok2c.lightnio.pool.ManagedIOSession;

class IOSessionReadyCallback implements FutureCallback<ManagedIOSession> {

    private final PendingDelivery pendingDelivery;

    public IOSessionReadyCallback(final PendingDelivery pendingDelivery) {
        super();
        this.pendingDelivery = pendingDelivery;
    }
    
    public void completed(final ManagedIOSession managedSession) {
        this.pendingDelivery.setManagedSession(managedSession);
        IOSession iosession = managedSession.getSession();
        iosession.setAttribute(PendingDeliveryHandler.PENDING_DELIVERY, this.pendingDelivery);
        iosession.setEvent(SelectionKey.OP_WRITE);
    }

    public void failed(final Exception ex) {
        this.pendingDelivery.getDeliveryFuture().failed(ex);
    }
    
    public void cancelled() {
        this.pendingDelivery.getDeliveryFuture().cancel(true);
    }
    
}
