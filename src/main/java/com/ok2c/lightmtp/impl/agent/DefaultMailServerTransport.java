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

import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.List;
import java.util.Set;

import com.ok2c.lightmtp.agent.MailServerTransport;
import com.ok2c.lightmtp.impl.protocol.ServerSession;
import com.ok2c.lightmtp.impl.protocol.ServerSessionFactory;
import com.ok2c.lightmtp.protocol.DeliveryHandler;
import com.ok2c.lightmtp.protocol.EnvelopValidator;
import com.ok2c.lightmtp.protocol.RemoteAddressValidator;
import com.ok2c.lightmtp.protocol.SessionFactory;
import com.ok2c.lightmtp.protocol.UniqueIdGenerator;
import com.ok2c.lightnio.IOReactorExceptionHandler;
import com.ok2c.lightnio.IOReactorStatus;
import com.ok2c.lightnio.ListenerEndpoint;
import com.ok2c.lightnio.ListeningIOReactor;
import com.ok2c.lightnio.impl.DefaultListeningIOReactor;
import com.ok2c.lightnio.impl.ExceptionEvent;
import com.ok2c.lightnio.impl.IOReactorConfig;

public class DefaultMailServerTransport extends AbstractMailTransport
                                        implements MailServerTransport {

    private final File workingDir;
    private final DefaultListeningIOReactor ioReactor;

    public DefaultMailServerTransport(
            final IOSessionRegistryCallback sessionRegistryCallback,
            final IOReactorThreadCallback reactorThreadCallback,
            final File workingDir,
            final IOReactorConfig config) throws IOException {
        super(sessionRegistryCallback, reactorThreadCallback);
        if (workingDir == null) {
            throw new IllegalArgumentException("Working dir may not be null");
        }
        this.workingDir = workingDir;
        this.ioReactor = new DefaultListeningIOReactor(config,
                new SimpleThreadFactory("MTA"));
    }

    public DefaultMailServerTransport(
            final File workingDir,
            final IOReactorConfig config) throws IOException {
        this(null, null, workingDir, config);
    }

    @Override
    protected ListeningIOReactor getIOReactor() {
        return this.ioReactor;
    }

    protected File getWorkingDir() {
        return this.workingDir;
    }

    public ListenerEndpoint listen(final SocketAddress address) {
        if (this.log.isDebugEnabled()) {
            this.log.debug("Start listener " + address);
        }
        return this.ioReactor.listen(address);
    }

    public Set<ListenerEndpoint> getEndpoints() {
        return this.ioReactor.getEndpoints();
    }

    public void setExceptionHandler(final IOReactorExceptionHandler exceptionHandler) {
        this.ioReactor.setExceptionHandler(exceptionHandler);
    }

    public void start(
            final UniqueIdGenerator idgenerator,
            final RemoteAddressValidator addressValidator,
            final EnvelopValidator envelopValidator,
            final DeliveryHandler deliveryHandler) {
        ServerSessionFactory sessionFactory = new ServerSessionFactory(
                this.workingDir,
                idgenerator,
                addressValidator,
                envelopValidator,
                deliveryHandler);
        start(sessionFactory);
    }

    public void start(
            final UniqueIdGenerator idgenerator,
            final EnvelopValidator envelopValidator,
            final DeliveryHandler deliveryHandler) {
        start(idgenerator, null, envelopValidator, deliveryHandler);
    }

    protected void start(final SessionFactory<ServerSession> sessionFactory) {
        ServerIOEventDispatch iodispatch = new ServerIOEventDispatch(
                getSessionRegistry(),
                sessionFactory);
        start(iodispatch);
    }

    public IOReactorStatus getStatus() {
        return this.ioReactor.getStatus();
    }

    public List<ExceptionEvent> getAuditLog() {
        return this.ioReactor.getAuditLog();
    }

    @Override
    public void shutdown() throws IOException {
        // Take down listeners
        this.ioReactor.pause();
        super.shutdown();
    }

}
