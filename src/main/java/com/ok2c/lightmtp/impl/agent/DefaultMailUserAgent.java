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
import java.nio.channels.SelectionKey;
import java.util.List;
import java.util.concurrent.Future;

import com.ok2c.lightmtp.agent.MailTransport;
import com.ok2c.lightmtp.agent.TransportType;
import com.ok2c.lightmtp.impl.protocol.ClientSession;
import com.ok2c.lightmtp.impl.protocol.ClientSessionFactory;
import com.ok2c.lightmtp.impl.protocol.LocalClientSessionFactory;
import com.ok2c.lightmtp.protocol.DeliveryRequest;
import com.ok2c.lightmtp.protocol.DeliveryRequestHandler;
import com.ok2c.lightmtp.protocol.DeliveryResult;
import com.ok2c.lightmtp.protocol.SessionContext;
import com.ok2c.lightmtp.protocol.SessionFactory;
import com.ok2c.lightnio.IOReactorExceptionHandler;
import com.ok2c.lightnio.IOReactorStatus;
import com.ok2c.lightnio.IOSession;
import com.ok2c.lightnio.concurrent.BasicFuture;
import com.ok2c.lightnio.concurrent.FutureCallback;
import com.ok2c.lightnio.impl.ExceptionEvent;
import com.ok2c.lightnio.impl.IOReactorConfig;
import com.ok2c.lightnio.pool.IOSessionManager;
import com.ok2c.lightnio.pool.ManagedIOSession;

public class DefaultMailUserAgent implements MailTransport {

    private static final String PENDING_DELIVERY = "com.ok2c.lightmtp.delivery";
    
    private final IOSessionManager<SocketAddress> sessionManager;
    private final DefaultMailClientTransport transport;    
    private final TransportType type;
    
    private volatile boolean shutdown;
    
    public DefaultMailUserAgent(
            final TransportType type,
            final IOReactorConfig config) throws IOException {
        super();
        this.type = type;
        this.transport = new DefaultMailClientTransport(
                new InternalIOSessionRegistryCallback(),
                new InternalIOReactorThreadCallback(),
                config);
        this.sessionManager = new MailIOSessionManager(this.transport.getIOReactor());
    }

    public void start() {
        DeliveryRequestHandler handler = new InternalDeliveryRequestHandler();
        SessionFactory<ClientSession> sessionFactory;
        switch (this.type) {
        case SMTP:
            sessionFactory = new ClientSessionFactory(handler);
            break;
        case LMTP:
            sessionFactory = new LocalClientSessionFactory(handler);
            break;
        default:
            sessionFactory = new ClientSessionFactory(handler);
        }
        
        this.transport.start(sessionFactory);
    }

    public Future<DeliveryResult> deliver(
            final InetSocketAddress address, 
            final DeliveryRequest request) {
        if (this.shutdown) {
            throw new IllegalStateException("Mail transport has been shut down");
        }
        BasicFuture<DeliveryResult> future = new BasicFuture<DeliveryResult>(null);
        PendingDelivery delivery = new PendingDelivery(request, future);
        this.sessionManager.leaseSession(address, null, new IOSessionReadyCallback(delivery));
        return future;
    }
    
    public void setExceptionHandler(final IOReactorExceptionHandler exceptionHandler) {
        this.transport.setExceptionHandler(exceptionHandler);
    }

    public IOReactorStatus getStatus() {
        return this.transport.getStatus();
    }

    public Exception getException() {
        return this.transport.getException();
    }

    public List<ExceptionEvent> getAuditLog() {
        return this.transport.getAuditLog();
    }

    public void shutdown() throws IOException {
        this.shutdown = true;
        this.transport.closeActiveSessions();
        this.sessionManager.shutdown();
        this.transport.shutdown();
    }

    public void forceShutdown() {
        this.transport.forceShutdown();
    }

    class InternalIOSessionRegistryCallback implements IOSessionRegistryCallback {

        public void removed(final IOSession iosession) {
            sessionManager.removeExpired(iosession);
        }
        
        public void added(final IOSession iosession) {
        }
    
    }
    
    class InternalIOReactorThreadCallback implements IOReactorThreadCallback {

        public void terminated() {
            shutdown = true;
        }

        public void terminated(Exception ex) {
            shutdown = true;
        }

    }
    
    class IOSessionReadyCallback implements FutureCallback<ManagedIOSession> {

        private final PendingDelivery pendingDelivery;

        public IOSessionReadyCallback(final PendingDelivery pendingDelivery) {
            super();
            this.pendingDelivery = pendingDelivery;
        }
        
        public void completed(final ManagedIOSession managedSession) {
            this.pendingDelivery.setManagedSession(managedSession);
            IOSession iosession = managedSession.getSession();
            iosession.setAttribute(PENDING_DELIVERY, this.pendingDelivery);
            iosession.setEvent(SelectionKey.OP_WRITE);
        }

        public void failed(final Exception ex) {
            this.pendingDelivery.getDeliveryFuture().failed(ex);
        }
        
        public void cancelled() {
            this.pendingDelivery.getDeliveryFuture().cancel(true);
        }
        
    }
    
    class InternalDeliveryRequestHandler implements DeliveryRequestHandler {

        public InternalDeliveryRequestHandler() {
            super();
        }

        public void connected(final SessionContext context) {
        }

        public void disconnected(final SessionContext context) {
            PendingDelivery delivery = (PendingDelivery) context.removeAttribute(PENDING_DELIVERY);
            if (delivery != null) {
                delivery.getDeliveryFuture().cancel(true);
                sessionManager.releaseSession(delivery.getManagedSession());
            }
        }

        public void exception(final Exception ex, final SessionContext context) {
            PendingDelivery delivery = (PendingDelivery) context.removeAttribute(PENDING_DELIVERY);
            if (delivery != null) {
                delivery.getDeliveryFuture().failed(ex);
                sessionManager.releaseSession(delivery.getManagedSession());
            }
        }

        public void completed(
                final DeliveryRequest request, 
                final DeliveryResult result, 
                final SessionContext context) {
            PendingDelivery delivery = (PendingDelivery) context.removeAttribute(PENDING_DELIVERY);
            if (delivery != null) {
                delivery.getDeliveryFuture().completed(result);
                sessionManager.releaseSession(delivery.getManagedSession());
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
}
