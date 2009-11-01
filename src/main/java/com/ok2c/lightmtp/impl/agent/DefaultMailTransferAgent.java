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

import com.ok2c.lightmtp.protocol.DeliveryHandler;
import com.ok2c.lightmtp.protocol.EnvelopValidator;
import com.ok2c.lightnio.IOEventDispatch;
import com.ok2c.lightnio.IOReactorExceptionHandler;
import com.ok2c.lightnio.IOReactorStatus;
import com.ok2c.lightnio.ListenerEndpoint;
import com.ok2c.lightnio.impl.DefaultListeningIOReactor;
import com.ok2c.lightnio.impl.ExceptionEvent;
import com.ok2c.lightnio.impl.IOReactorConfig;

public class DefaultMailTransferAgent {

    private final DefaultListeningIOReactor ioReactor;
    private final String serverId;
    private final File workingDir;
    private final EnvelopValidator validator;
    private final DeliveryHandler handler;

    private volatile IOReactorThread thread;

    public DefaultMailTransferAgent(
            final String serverId,
            final File workingDir,
            final EnvelopValidator validator,
            final DeliveryHandler handler,
            final IOReactorConfig config) throws IOException {
        super();
        if (workingDir == null) {
            throw new IllegalArgumentException("Working dir may not be null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Delivery handler may not be null");
        }
        this.serverId = serverId;
        this.workingDir = workingDir;
        this.validator = validator;
        this.handler = handler;
        this.ioReactor = new DefaultListeningIOReactor(config);
    }

    public ListenerEndpoint listen(final SocketAddress address) {
        return this.ioReactor.listen(address);
    }

    public Set<ListenerEndpoint> getEndpoints() {
        return this.ioReactor.getEndpoints();
    }

    public void setExceptionHandler(final IOReactorExceptionHandler exceptionHandler) {
        this.ioReactor.setExceptionHandler(exceptionHandler);
    }

    protected IOEventDispatch createIOEventDispatch(
            final String serverId,
            final File workingDir,
            final EnvelopValidator validator,
            final DeliveryHandler handler) {
        return new ServerIOEventDispatch(serverId, workingDir, validator, handler);
    }

    private void execute() throws IOException {
        IOEventDispatch ioEventDispatch = createIOEventDispatch(
                this.serverId,
                this.workingDir,
                this.validator,
                this.handler);
        this.ioReactor.execute(ioEventDispatch);
    }

    public void start() {
        this.thread = new IOReactorThread();
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
        this.ioReactor.shutdown();
        IOReactorThread t = this.thread;
        try {
            if (t != null) {
                t.join(500);
            }
        } catch (InterruptedException ignore) {
        }
    }

    private class IOReactorThread extends Thread {

        private volatile Exception ex;

        @Override
        public void run() {
            try {
                execute();
            } catch (Exception ex) {
                this.ex = ex;
            }
        }

        public Exception getException() {
            return this.ex;
        }

    }

}
