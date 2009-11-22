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
import com.ok2c.lightmtp.protocol.SessionFactory;
import com.ok2c.lightnio.IOReactorExceptionHandler;
import com.ok2c.lightnio.IOReactorStatus;
import com.ok2c.lightnio.IOSession;
import com.ok2c.lightnio.concurrent.BasicFuture;
import com.ok2c.lightnio.impl.ExceptionEvent;
import com.ok2c.lightnio.impl.IOReactorConfig;
import com.ok2c.lightnio.pool.IOSessionManager;

public class DefaultMailUserAgent implements MailTransport {

    private final IOSessionManager<SocketAddress> sessionManager;
    private final DefaultMailClientTransport transport;    
    private final TransportType type;
    
    public DefaultMailUserAgent(
            final TransportType type,
            final IOReactorConfig config) throws IOException {
        super();
        this.type = type;
        this.transport = new DefaultMailClientTransport(
                new IOSessionRegistryCallback() {
                    
                    public void removed(final IOSession iosession) {
                        sessionManager.removeExpired(iosession);
                    }
                    
                    public void added(final IOSession iosession) {
                    }
                    
                },
                config);
        this.sessionManager = new MailIOSessionManager(this.transport.getIOReactor());
    }

    public void start() {
        DeliveryRequestHandler handler = new PendingDeliveryHandler(this.sessionManager);
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
        this.transport.closeActiveSessions();
        this.sessionManager.shutdown();
        this.transport.shutdown();
    }

    public void forceShutdown() {
        this.transport.forceShutdown();
    }
    
}
