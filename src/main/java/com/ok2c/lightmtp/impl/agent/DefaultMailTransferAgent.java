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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ok2c.lightmtp.agent.IOEventDispatchFactory;
import com.ok2c.lightmtp.protocol.DeliveryHandler;
import com.ok2c.lightmtp.protocol.EnvelopValidator;
import com.ok2c.lightnio.IOReactorExceptionHandler;
import com.ok2c.lightnio.IOReactorStatus;
import com.ok2c.lightnio.ListenerEndpoint;
import com.ok2c.lightnio.impl.DefaultListeningIOReactor;
import com.ok2c.lightnio.impl.ExceptionEvent;
import com.ok2c.lightnio.impl.IOReactorConfig;

public class DefaultMailTransferAgent {

    private final File workingDir;
    private final DefaultListeningIOReactor ioReactor;

    private final Log log;
    
    private volatile IOReactorThread thread;

    public DefaultMailTransferAgent(
            final File workingDir,
            final IOReactorConfig config) throws IOException {
        super();
        if (workingDir == null) {
            throw new IllegalArgumentException("Working dir may not be null");
        }
        this.workingDir = workingDir;
        this.ioReactor = new DefaultListeningIOReactor(config);

        this.log = LogFactory.getLog(getClass());
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

    public void start(final IOEventDispatchFactory dispatchFactory) {
        this.log.debug("Start I/O reactor");
        this.thread = new IOReactorThread(this.ioReactor, dispatchFactory);
        this.thread.start();
    }

    public void start(
            final EnvelopValidator envelopValidator,
            final DeliveryHandler deliveryHandler) {
        start(new ServerIOEventDispatchFactory(
                this.workingDir, 
                envelopValidator, 
                deliveryHandler));
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
