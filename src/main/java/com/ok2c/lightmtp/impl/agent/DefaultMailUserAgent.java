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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import com.ok2c.lightmtp.agent.MailUserAgent;
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

public class DefaultMailUserAgent implements MailUserAgent {

    private static final String PENDING_DELIVERY = "com.ok2c.lightmtp.delivery";

    private final IOSessionManager<SocketAddress> sessionManager;
    private final DefaultMailClientTransport transport;
    private final TransportType type;
    private final Set<PendingDelivery> pendingDeliveries;

    private volatile boolean started;

    private volatile boolean shutdown;

	private String heloName;

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
        this.pendingDeliveries = Collections.synchronizedSet(new HashSet<PendingDelivery>());
    }

    public void start() {
        started = true;
        DeliveryRequestHandler handler = new InternalDeliveryRequestHandler();
        SessionFactory<ClientSession> sessionFactory;
        switch (this.type) {
        case SMTP:
            sessionFactory = new ClientSessionFactory(handler, heloName);
            break;
        case LMTP:
            sessionFactory = new LocalClientSessionFactory(handler, heloName);
            break;
        default:
            sessionFactory = new ClientSessionFactory(handler);
        }

        this.transport.start(sessionFactory);
    }

    /**
     * Set the helo name to use. Must be called before {@link #start()} to take affect
     * 
     * @param heloName
     */
    public void setHeloName(String heloName) {
        if (started) throw new IllegalStateException("Can only be set when not started");
        this.heloName = heloName;
    }
    
    public Future<DeliveryResult> deliver(
            final InetSocketAddress address,
            final DeliveryRequest request,
            final FutureCallback<DeliveryResult> callback) {
        return deliver(address, null,  request, callback);
    }
    
    public Future<DeliveryResult> deliver(
            final InetSocketAddress address,
            final InetSocketAddress localAdress,
            final DeliveryRequest request,
            final FutureCallback<DeliveryResult> callback) {
        if (this.shutdown) {
            throw new IllegalStateException("Mail transport has been shut down");
        }
        BasicFuture<DeliveryResult> future = new BasicFuture<DeliveryResult>(callback);
        PendingDelivery delivery = new PendingDelivery(request, future);
        this.pendingDeliveries.add(delivery);
        this.sessionManager.leaseSession(address, localAdress, new IOSessionReadyCallback(delivery));
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
        this.started = false;
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

        private void cancelDeliveries() {
            synchronized (pendingDeliveries) {
                for (PendingDelivery delivery: pendingDeliveries) {
                    delivery.getDeliveryFuture().cancel(true);
                }
                pendingDeliveries.clear();
            }
        }

        public void terminated() {
            shutdown = true;
            started = false;
            cancelDeliveries();
        }

        public void terminated(final Exception ex) {
            shutdown = true;
            started = false;
            cancelDeliveries();
        }

    }

    class IOSessionReadyCallback implements FutureCallback<ManagedIOSession> {

        private final PendingDelivery pendingDelivery;

        public IOSessionReadyCallback(final PendingDelivery pendingDelivery) {
            super();
            this.pendingDelivery = pendingDelivery;
        }

        private void deliveryDone() {
            pendingDeliveries.remove(this.pendingDelivery);
        }

        public void completed(final ManagedIOSession managedSession) {
            deliveryDone();
            this.pendingDelivery.setManagedSession(managedSession);
            IOSession iosession = managedSession.getSession();
            iosession.setAttribute(PENDING_DELIVERY, this.pendingDelivery);
            iosession.setEvent(SelectionKey.OP_WRITE);
        }

        public void failed(final Exception ex) {
            deliveryDone();
            this.pendingDelivery.getDeliveryFuture().failed(ex);
        }

        public void cancelled() {
            deliveryDone();
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
