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
import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ok2c.lightmtp.agent.MailTransport;
import com.ok2c.lightmtp.impl.protocol.ProtocolState;
import com.ok2c.lightnio.IOEventDispatch;
import com.ok2c.lightnio.IOReactor;
import com.ok2c.lightnio.IOSession;

abstract class AbstractMailTransport implements MailTransport {

    protected final Log log = LogFactory.getLog(getClass());

    private final IOSessionRegistry sessionRegistry;
    private final IOReactorThreadCallback reactorThreadCallback;
    
    private volatile IOReactorThread thread;

    public AbstractMailTransport(
            final IOSessionRegistryCallback sessionRegistryCallback,
            final IOReactorThreadCallback reactorThreadCallback) {
        super();
        this.sessionRegistry = new IOSessionRegistry(sessionRegistryCallback);
        this.reactorThreadCallback = reactorThreadCallback;
    }
    
    protected abstract IOReactor getIOReactor();
    
    protected IOSessionRegistry getSessionRegistry() {
        return this.sessionRegistry;
    }
    
    protected void start(final IOEventDispatch iodispatch) {
        this.log.debug("Start I/O reactor");
        this.thread = new IOReactorThread(getIOReactor(), iodispatch, this.reactorThreadCallback);
        this.thread.start();
    }

    public Exception getException() {
        IOReactorThread t = this.thread;
        if (t != null) {
            return t.getException();
        } else {
            return null;
        }
    }
    
    protected void closeActiveSessions(
            int timeout, final TimeUnit unit) throws InterruptedException {
        this.log.debug("Terminate active sessions");
        synchronized (this.sessionRegistry) {
            Iterator<IOSession> sessions = this.sessionRegistry.iterator();
            while (sessions.hasNext()) {
                IOSession session = sessions.next(); 
                session.setAttribute(ProtocolState.ATTRIB, ProtocolState.QUIT);
                session.setEvent(SelectionKey.OP_WRITE);
            }
            long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
            long remaining = timeout;
            boolean empty = false;
            while (!empty) {
                this.sessionRegistry.wait(remaining);
                empty = this.sessionRegistry.isEmpty();            
                if (timeout > 0) {
                    remaining = deadline - System.currentTimeMillis();
                    if (remaining <= 0) {
                        break;
                    }
                }
            }
            if (!empty && this.log.isWarnEnabled()) {
                this.log.warn("Failed to shut down active sessions within " 
                        + timeout + " "  + unit);
            }
        }
    }

    public void shutdown() throws IOException {
        try {
            closeActiveSessions(30, TimeUnit.SECONDS);
        } catch (InterruptedException ignore) {
        }
        forceShutdown(30000);        
    }

    protected void forceShutdown(long gracePeriod) throws IOException {
        this.log.debug("Shut down I/O reactor");
        getIOReactor().shutdown(gracePeriod);
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
