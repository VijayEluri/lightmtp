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

import java.net.SocketAddress;

import com.ok2c.lightmtp.protocol.DeliveryRequest;
import com.ok2c.lightmtp.protocol.DeliveryRequestHandler;
import com.ok2c.lightmtp.protocol.DeliveryResult;
import com.ok2c.lightmtp.protocol.SessionContext;
import com.ok2c.lightnio.pool.IOSessionManager;

class PendingDeliveryHandler implements DeliveryRequestHandler {

    static final String PENDING_DELIVERY = "com.ok2c.lightmtp.delivery";
    
    final IOSessionManager<SocketAddress> sessionManager;
    
    public PendingDeliveryHandler(final IOSessionManager<SocketAddress> sessionManager) {
        super();
        this.sessionManager = sessionManager;
    }

    public void connected(final SessionContext context) {
    }

    public void disconnected(final SessionContext context) {
        PendingDelivery delivery = (PendingDelivery) context.removeAttribute(PENDING_DELIVERY);
        if (delivery != null) {
            delivery.getDeliveryFuture().cancel(true);
            this.sessionManager.releaseSession(delivery.getManagedSession());
        }
    }

    public void exception(final Exception ex, final SessionContext context) {
        PendingDelivery delivery = (PendingDelivery) context.removeAttribute(PENDING_DELIVERY);
        if (delivery != null) {
            delivery.getDeliveryFuture().failed(ex);
            this.sessionManager.releaseSession(delivery.getManagedSession());
        }
    }

    public void completed(
            final DeliveryRequest request, 
            final DeliveryResult result, 
            final SessionContext context) {
        PendingDelivery delivery = (PendingDelivery) context.removeAttribute(PENDING_DELIVERY);
        if (delivery != null) {
            delivery.getDeliveryFuture().completed(result);
            this.sessionManager.releaseSession(delivery.getManagedSession());
        }
    }

    public void failed(
            final DeliveryRequest request, 
            final DeliveryResult result, 
            final SessionContext context) {
        completed(request, result, context);
    }

    public DeliveryRequest submitRequest(final SessionContext context) {
        PendingDelivery delivery = (PendingDelivery) context.getAttribute(PENDING_DELIVERY);
        if (delivery != null) {
            return delivery.getRequest();
        } else {
            return null;
        }
    }
    
}
