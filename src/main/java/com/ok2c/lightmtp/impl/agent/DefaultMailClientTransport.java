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
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.ok2c.lightmtp.agent.MailClientTransport;
import com.ok2c.lightmtp.impl.protocol.ClientSession;
import com.ok2c.lightmtp.impl.protocol.ClientSessionFactory;
import com.ok2c.lightmtp.protocol.DeliveryRequestHandler;
import com.ok2c.lightmtp.protocol.SessionFactory;
import com.ok2c.lightnio.ConnectingIOReactor;
import com.ok2c.lightnio.IOReactorExceptionHandler;
import com.ok2c.lightnio.IOReactorStatus;
import com.ok2c.lightnio.SessionRequest;
import com.ok2c.lightnio.SessionRequestCallback;
import com.ok2c.lightnio.impl.DefaultConnectingIOReactor;
import com.ok2c.lightnio.impl.ExceptionEvent;
import com.ok2c.lightnio.impl.IOReactorConfig;

public class DefaultMailClientTransport extends AbstractMailTransport 
                                        implements MailClientTransport {

    private final DefaultConnectingIOReactor ioReactor;

    public DefaultMailClientTransport(
            final IOSessionRegistry sessionRegistry,
            final IOReactorConfig config) throws IOException {
        super(sessionRegistry);
        this.ioReactor = new DefaultConnectingIOReactor(config, 
                new SimpleThreadFactory("MTU"));
    }

    public DefaultMailClientTransport(
            final IOReactorConfig config) throws IOException {
        this(new IOSessionRegistry(), config);
    }

    @Override
    protected ConnectingIOReactor getIOReactor() {
        return this.ioReactor;
    }
    
    public void setExceptionHandler(final IOReactorExceptionHandler exceptionHandler) {
        this.ioReactor.setExceptionHandler(exceptionHandler);
    }

    public SessionRequest connect(
            final SocketAddress remoteAddress,
            final Object attachment,
            final SessionRequestCallback callback) {
        if (this.log.isDebugEnabled()) {
            this.log.debug("Request new connection " + remoteAddress + " [" + attachment + "]");
        }
        return this.ioReactor.connect(remoteAddress, null, attachment, callback);
    }
    
    protected void start(final SessionFactory<ClientSession> sessionFactory) {
        ClientIOEventDispatch iodispatch = new ClientIOEventDispatch(
                getSessionRegistry(), 
                sessionFactory);
        start(iodispatch);
    }

    public void start(final DeliveryRequestHandler deliveryRequestHandler) {
        ClientSessionFactory sessionFactory = new ClientSessionFactory(deliveryRequestHandler);
        start(sessionFactory);
    }

    public IOReactorStatus getStatus() {
        return this.ioReactor.getStatus();
    }

    public List<ExceptionEvent> getAuditLog() {
        return this.ioReactor.getAuditLog();
    }

    public void closeActiveSessions() {
        try {
            closeActiveSessions(30, TimeUnit.SECONDS);
        } catch (InterruptedException ignore) {
        }
    }

}
