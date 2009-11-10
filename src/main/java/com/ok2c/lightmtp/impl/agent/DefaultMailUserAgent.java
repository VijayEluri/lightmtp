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
import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ok2c.lightmtp.agent.IOSessionRegistry;
import com.ok2c.lightmtp.impl.protocol.ClientSession;
import com.ok2c.lightmtp.impl.protocol.ClientSessionFactory;
import com.ok2c.lightmtp.impl.protocol.ProtocolState;
import com.ok2c.lightmtp.protocol.DeliveryRequestHandler;
import com.ok2c.lightmtp.protocol.SessionFactory;
import com.ok2c.lightnio.IOReactorExceptionHandler;
import com.ok2c.lightnio.IOReactorStatus;
import com.ok2c.lightnio.IOSession;
import com.ok2c.lightnio.SessionRequest;
import com.ok2c.lightnio.SessionRequestCallback;
import com.ok2c.lightnio.impl.DefaultConnectingIOReactor;
import com.ok2c.lightnio.impl.ExceptionEvent;
import com.ok2c.lightnio.impl.IOReactorConfig;

public class DefaultMailUserAgent {

    private final DefaultConnectingIOReactor ioReactor;
    private final IOSessionRegistry sessionRegistry;

    private final Log log;
    
    private volatile IOReactorThread thread;

    public DefaultMailUserAgent(
            final IOReactorConfig config) throws IOException {
        super();
        this.ioReactor = new DefaultConnectingIOReactor(config, 
                new SimpleThreadFactory("MTU"));
        this.sessionRegistry = new IOSessionRegistry();
        
        this.log = LogFactory.getLog(getClass());
    }

    public void setExceptionHandler(final IOReactorExceptionHandler exceptionHandler) {
        this.ioReactor.setExceptionHandler(exceptionHandler);
    }

    public SessionRequest connect(
            final SocketAddress remoteAddress,
            final SocketAddress localAddress,
            final Object attachment,
            final SessionRequestCallback callback) {
        if (this.log.isDebugEnabled()) {
            this.log.debug("Request new connection " + remoteAddress + " [" + attachment + "]");
        }
        return this.ioReactor.connect(remoteAddress, localAddress, attachment, callback);
    }
    
    public void start(final SessionFactory<ClientSession> sessionFactory) {
        this.log.debug("Start I/O reactor");
        
        ClientIOEventDispatch iodispatch = new ClientIOEventDispatch(
                this.sessionRegistry, 
                sessionFactory);
        this.thread = new IOReactorThread(this.ioReactor, iodispatch);
        this.thread.start();
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
        
        synchronized (this.sessionRegistry) {
            Iterator<IOSession> sessions = this.sessionRegistry.iterator();
            while (sessions.hasNext()) {
                IOSession session = sessions.next(); 
                session.setAttribute(ClientSession.TERMINATE, ProtocolState.QUIT);
                session.setEvent(SelectionKey.OP_WRITE);
            }
        }
        try {
            if (!this.sessionRegistry.awaitShutdown(30, TimeUnit.SECONDS)) {
                this.log.warn("Failed to shut down active sessions within 30 seconds");
            }
        } catch (InterruptedException ignore) {
        }
        
        this.ioReactor.shutdown(30000);
        IOReactorThread t = this.thread;
        try {
            if (t != null) {
                t.join(1000);
            }
        } catch (InterruptedException ignore) {
        }
    }

}
