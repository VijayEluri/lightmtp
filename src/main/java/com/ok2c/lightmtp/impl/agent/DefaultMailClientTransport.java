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
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.ExceptionEvent;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorExceptionHandler;
import org.apache.http.nio.reactor.IOReactorStatus;
import org.apache.http.nio.reactor.SessionRequest;
import org.apache.http.nio.reactor.SessionRequestCallback;

import com.ok2c.lightmtp.agent.MailClientTransport;
import com.ok2c.lightmtp.agent.SessionEndpoint;
import com.ok2c.lightmtp.impl.protocol.ClientSession;
import com.ok2c.lightmtp.impl.protocol.ClientSessionFactory;
import com.ok2c.lightmtp.protocol.DeliveryRequestHandler;
import com.ok2c.lightmtp.protocol.SessionFactory;

public class DefaultMailClientTransport extends AbstractMailTransport
                                        implements MailClientTransport {

    private final DefaultConnectingIOReactor ioReactor;

    public DefaultMailClientTransport(
            final IOSessionRegistryCallback sessionRegistryCallback,
            final IOReactorThreadCallback reactorThreadCallback,
            final IOReactorConfig config) throws IOException {
        super(sessionRegistryCallback, reactorThreadCallback);
        this.ioReactor = new DefaultConnectingIOReactor(config,
                new SimpleThreadFactory("MUA"));
    }

    public DefaultMailClientTransport(
            final IOReactorConfig config) throws IOException {
        this(null, null, config);
    }

    @Override
    protected ConnectingIOReactor getIOReactor() {
        return this.ioReactor;
    }

    public void setExceptionHandler(final IOReactorExceptionHandler exceptionHandler) {
        this.ioReactor.setExceptionHandler(exceptionHandler);
    }

    @Override
    public SessionRequest connect(
            final SessionEndpoint endpoint,
            final Object attachment,
            final SessionRequestCallback callback) {
        if (this.log.isDebugEnabled()) {
            this.log.debug("Request new connection to " + endpoint);
        }
        return this.ioReactor.connect(endpoint.getRemoteAddress(), endpoint.getLocalAddress(),
                attachment, callback);
    }

    protected void start(final SessionFactory<ClientSession> sessionFactory) {
        ClientIOEventDispatch iodispatch = new ClientIOEventDispatch(
                getSessionRegistry(),
                sessionFactory);
        start(iodispatch);
    }

    @Override
    public void start(final DeliveryRequestHandler deliveryRequestHandler) {
        ClientSessionFactory sessionFactory = new ClientSessionFactory(deliveryRequestHandler);
        start(sessionFactory);
    }

    @Override
    public IOReactorStatus getStatus() {
        return this.ioReactor.getStatus();
    }

    @Override
    public List<ExceptionEvent> getAuditLog() {
        return this.ioReactor.getAuditLog();
    }

    @Override
    public void closeActiveSessions() {
        try {
            closeActiveSessions(30, TimeUnit.SECONDS);
        } catch (InterruptedException ignore) {
        }
    }

}
