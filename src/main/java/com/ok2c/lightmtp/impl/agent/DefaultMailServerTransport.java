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
import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ok2c.lightmtp.agent.MailServerTransport;
import com.ok2c.lightmtp.impl.protocol.ProtocolState;
import com.ok2c.lightmtp.impl.protocol.ServerSession;
import com.ok2c.lightmtp.impl.protocol.ServerSessionFactory;
import com.ok2c.lightmtp.protocol.DeliveryHandler;
import com.ok2c.lightmtp.protocol.EnvelopValidator;
import com.ok2c.lightmtp.protocol.SessionFactory;
import com.ok2c.lightnio.IOReactorExceptionHandler;
import com.ok2c.lightnio.IOReactorStatus;
import com.ok2c.lightnio.IOSession;
import com.ok2c.lightnio.ListenerEndpoint;
import com.ok2c.lightnio.impl.DefaultListeningIOReactor;
import com.ok2c.lightnio.impl.ExceptionEvent;
import com.ok2c.lightnio.impl.IOReactorConfig;

public class DefaultMailServerTransport implements MailServerTransport {

    private final File workingDir;
    private final DefaultListeningIOReactor ioReactor;
    private final IOSessionRegistry sessionRegistry;

    private final Log log;
    
    private volatile IOReactorThread thread;

    public DefaultMailServerTransport(
            final File workingDir,
            final IOReactorConfig config) throws IOException {
        super();
        if (workingDir == null) {
            throw new IllegalArgumentException("Working dir may not be null");
        }
        this.workingDir = workingDir;
        this.ioReactor = new DefaultListeningIOReactor(config,
                new SimpleThreadFactory("MUA"));
        this.sessionRegistry = new IOSessionRegistry();

        this.log = LogFactory.getLog(getClass());
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
            final EnvelopValidator envelopValidator,
            final DeliveryHandler deliveryHandler) {
        ServerSessionFactory sessionFactory = new ServerSessionFactory(
                this.workingDir,
                envelopValidator,
                deliveryHandler);
        start(sessionFactory);
    }

    protected void start(final SessionFactory<ServerSession> sessionFactory) {
        this.log.debug("Start I/O reactor");
        
        ServerIOEventDispatch iodispatch = new ServerIOEventDispatch(
                this.sessionRegistry, 
                sessionFactory);
        this.thread = new IOReactorThread(this.ioReactor, iodispatch);
        this.thread.start();
    }

    public IOReactorStatus getStatus() {
        return this.ioReactor.getStatus();
    }

    public List<ExceptionEvent> getAuditLog() {
        return this.ioReactor.getAuditLog();
    }

    public Exception getException() {
        IOReactorThread t = this.thread;
        if (t != null) {
            return t.getException();
        } else {
            return null;
        }
    }

    public void shutdown() throws IOException {
        this.log.debug("Shut down I/O reactor");
        // Take down listeners
        this.ioReactor.pause();
        synchronized (this.sessionRegistry) {
            Iterator<IOSession> sessions = this.sessionRegistry.iterator();
            while (sessions.hasNext()) {
                IOSession session = sessions.next(); 
                session.setAttribute(ServerSession.TERMINATE, ProtocolState.QUIT);
                session.setEvent(SelectionKey.OP_WRITE);
            }
        }
        try {
            if (!this.sessionRegistry.awaitShutdown(30, TimeUnit.SECONDS)) {
                this.log.warn("Failed to shut down active sessions within 30 seconds");
            }
        } catch (InterruptedException ignore) {
        }
        forceShutdown(30000);        
    }

    protected void forceShutdown(long gracePeriod) throws IOException {
        this.ioReactor.shutdown(gracePeriod);
        IOReactorThread t = this.thread;
        try {
            if (t != null) {
                t.join(1000);
            }
        } catch (InterruptedException ignore) {
        }
    }

    public void forceShutdown() {
        try {
            forceShutdown(1000);
        } catch (IOException ignore) {
        }
    }
    
}
