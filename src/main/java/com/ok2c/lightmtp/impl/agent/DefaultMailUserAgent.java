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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ok2c.lightmtp.protocol.DeliveryRequestHandler;
import com.ok2c.lightnio.IOEventDispatch;
import com.ok2c.lightnio.IOReactorExceptionHandler;
import com.ok2c.lightnio.IOReactorStatus;
import com.ok2c.lightnio.SessionRequest;
import com.ok2c.lightnio.SessionRequestCallback;
import com.ok2c.lightnio.impl.DefaultConnectingIOReactor;
import com.ok2c.lightnio.impl.ExceptionEvent;
import com.ok2c.lightnio.impl.IOReactorConfig;

public class DefaultMailUserAgent {

    private final DefaultConnectingIOReactor ioReactor;

    private final Log log;
    
    private volatile IOReactorThread thread;

    public DefaultMailUserAgent(
            final IOReactorConfig config) throws IOException {
        super();
        this.ioReactor = new DefaultConnectingIOReactor(config);

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
    
    protected IOEventDispatch createIOEventDispatch(
            final DeliveryRequestHandler handler) {
        return new ClientIOEventDispatch(handler);
    }

    private void execute(
            final DeliveryRequestHandler handler) throws IOException {
        IOEventDispatch ioEventDispatch = createIOEventDispatch(handler);
        this.ioReactor.execute(ioEventDispatch);
    }

    public void start(
            final DeliveryRequestHandler handler) {
        this.log.debug("Start I/O reactor");
        this.thread = new IOReactorThread(handler);
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
        this.ioReactor.shutdown(30000);
        IOReactorThread t = this.thread;
        try {
            if (t != null) {
                t.join(1000);
            }
        } catch (InterruptedException ignore) {
        }
    }

    private class IOReactorThread extends Thread {

        private final DeliveryRequestHandler handler;

        private volatile Exception ex;

        public IOReactorThread(final DeliveryRequestHandler handler) {
            super();
            this.handler = handler;
        }
        @Override
        public void run() {
            try {
                execute(this.handler);
            } catch (Exception ex) {
                this.ex = ex;
            }
        }

        public Exception getException() {
            return this.ex;
        }

    }

}
