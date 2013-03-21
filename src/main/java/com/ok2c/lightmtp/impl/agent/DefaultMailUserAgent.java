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
import java.nio.channels.SelectionKey;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.http.concurrent.BasicFuture;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.reactor.ExceptionEvent;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.reactor.IOReactorExceptionHandler;
import org.apache.http.nio.reactor.IOReactorStatus;
import org.apache.http.nio.reactor.IOSession;

import com.ok2c.lightmtp.agent.MailUserAgent;
import com.ok2c.lightmtp.agent.SessionEndpoint;
import com.ok2c.lightmtp.agent.TransportType;
import com.ok2c.lightmtp.impl.pool.LeasedSession;
import com.ok2c.lightmtp.impl.pool.MailIOSessionManager;
import com.ok2c.lightmtp.impl.protocol.ClientSession;
import com.ok2c.lightmtp.impl.protocol.ClientSessionFactory;
import com.ok2c.lightmtp.impl.protocol.LocalClientSessionFactory;
import com.ok2c.lightmtp.protocol.DeliveryRequest;
import com.ok2c.lightmtp.protocol.DeliveryRequestHandler;
import com.ok2c.lightmtp.protocol.DeliveryResult;
import com.ok2c.lightmtp.protocol.SessionContext;
import com.ok2c.lightmtp.protocol.SessionFactory;

public class DefaultMailUserAgent implements MailUserAgent {

    private static final String PENDING_DELIVERY = "com.ok2c.lightmtp.delivery";

    private final MailIOSessionManager sessionManager;
    private final DefaultMailClientTransport transport;
    private final TransportType type;
    private final Set<PendingDelivery> pendingDeliveries;

    private volatile boolean started;

    private volatile boolean shutdown;

	private String heloName;

    private String username;

    private String password;

    public DefaultMailUserAgent(
            final TransportType type,
            final IOReactorConfig config) throws IOException {
        super();
        this.type = type;
        this.transport = new DefaultMailClientTransport(
                null,
                new InternalIOReactorThreadCallback(),
                config);
        this.sessionManager = new MailIOSessionManager(this.transport.getIOReactor());
        this.pendingDeliveries = Collections.synchronizedSet(new HashSet<PendingDelivery>());
    }

    @Override
    public void start() {
        started = true;
        DeliveryRequestHandler handler = new InternalDeliveryRequestHandler();
        SessionFactory<ClientSession> sessionFactory;
        switch (this.type) {
        case SMTP:
            sessionFactory = new ClientSessionFactory(handler, heloName, username, password);
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
    public void setHeloName(final String heloName) {
        if (started) throw new IllegalStateException("Can only be set when not started");
        this.heloName = heloName;
    }

    /**
     * Set the authentication to use. Must be called before {@link #start()}
     *
     * @param username
     * @param password
     */
    public void setAuthentication(final String username, final String password) {
        if (started) throw new IllegalStateException("Can only be set when not started");
        if ((username == null || password == null) && (username != null || password != null)) {
            throw new IllegalArgumentException("You need to set username and password to null or none of them");
        }
        this.username = username;
        this.password = password;
    }

    public Future<DeliveryResult> deliver(
            final InetSocketAddress remoteAddress,
            final DeliveryRequest request,
            final FutureCallback<DeliveryResult> callback) {
        return deliver(remoteAddress, request, callback);
    }

    @Override
    public Future<DeliveryResult> deliver(
            final SessionEndpoint endpoint,
            final int connectTimeout,
            final DeliveryRequest request,
            final FutureCallback<DeliveryResult> callback) {
        if (this.shutdown) {
            throw new IllegalStateException("Mail transport has been shut down");
        }
        BasicFuture<DeliveryResult> future = new BasicFuture<DeliveryResult>(callback);
        PendingDelivery delivery = new PendingDelivery(request, future);
        this.pendingDeliveries.add(delivery);
        this.sessionManager.leaseSession(endpoint,
                connectTimeout, TimeUnit.MILLISECONDS,
                new IOSessionReadyCallback(delivery));
        return future;
    }

    public void setExceptionHandler(final IOReactorExceptionHandler exceptionHandler) {
        this.transport.setExceptionHandler(exceptionHandler);
    }

    @Override
    public IOReactorStatus getStatus() {
        return this.transport.getStatus();
    }

    @Override
    public Exception getException() {
        return this.transport.getException();
    }

    @Override
    public List<ExceptionEvent> getAuditLog() {
        return this.transport.getAuditLog();
    }

    @Override
    public void shutdown() throws IOException {
        this.shutdown = true;
        this.started = false;
        this.transport.closeActiveSessions();
        this.sessionManager.shutdown();
        this.transport.shutdown();
    }

    @Override
    public void forceShutdown() {
        this.transport.forceShutdown();
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

        @Override
        public void terminated() {
            shutdown = true;
            started = false;
            cancelDeliveries();
        }

        @Override
        public void terminated(final Exception ex) {
            shutdown = true;
            started = false;
            cancelDeliveries();
        }

    }

    class IOSessionReadyCallback implements FutureCallback<LeasedSession> {

        private final PendingDelivery pendingDelivery;

        public IOSessionReadyCallback(final PendingDelivery pendingDelivery) {
            super();
            this.pendingDelivery = pendingDelivery;
        }

        private void deliveryDone() {
            pendingDeliveries.remove(this.pendingDelivery);
        }

        @Override
        public void completed(final LeasedSession leasedSession) {
            deliveryDone();
            this.pendingDelivery.setLeasedSession(leasedSession);
            IOSession iosession = leasedSession.getIOSession();
            iosession.setAttribute(PENDING_DELIVERY, this.pendingDelivery);
            iosession.setEvent(SelectionKey.OP_WRITE);
        }

        @Override
        public void failed(final Exception ex) {
            deliveryDone();
            this.pendingDelivery.getDeliveryFuture().failed(ex);
        }

        @Override
        public void cancelled() {
            deliveryDone();
            this.pendingDelivery.getDeliveryFuture().cancel(true);
        }

    }

    class InternalDeliveryRequestHandler implements DeliveryRequestHandler {

        public InternalDeliveryRequestHandler() {
            super();
        }

        @Override
        public void connected(final SessionContext context) {
        }

        @Override
        public void disconnected(final SessionContext context) {
            PendingDelivery delivery = (PendingDelivery) context.removeAttribute(PENDING_DELIVERY);
            if (delivery != null) {
                delivery.getDeliveryFuture().cancel(true);
                sessionManager.releaseSession(delivery.getLeasedSession());
            }
        }

        @Override
        public void exception(final Exception ex, final SessionContext context) {
            PendingDelivery delivery = (PendingDelivery) context.removeAttribute(PENDING_DELIVERY);
            if (delivery != null) {
                delivery.getDeliveryFuture().failed(ex);
                sessionManager.releaseSession(delivery.getLeasedSession());
            }
        }

        @Override
        public void completed(
                final DeliveryRequest request,
                final DeliveryResult result,
                final SessionContext context) {
            PendingDelivery delivery = (PendingDelivery) context.removeAttribute(PENDING_DELIVERY);
            if (delivery != null) {
                delivery.getDeliveryFuture().completed(result);
                sessionManager.releaseSession(delivery.getLeasedSession());
            }
        }

        @Override
        public void failed(
                final DeliveryRequest request,
                final DeliveryResult result,
                final SessionContext context) {
            completed(request, result, context);
        }

        @Override
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
