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

import com.ok2c.lightmtp.protocol.DeliveryRequestHandler;
import com.ok2c.lightnio.IOEventDispatch;
import com.ok2c.lightnio.IOReactorExceptionHandler;
import com.ok2c.lightnio.IOReactorStatus;
import com.ok2c.lightnio.impl.DefaultConnectingIOReactor;
import com.ok2c.lightnio.impl.ExceptionEvent;
import com.ok2c.lightnio.impl.IOReactorConfig;

public class DefaultMailUserAgent {

    private final DefaultConnectingIOReactor ioReactor;
    private final DeliveryRequestHandler handler;

    private volatile IOReactorThread thread;

    public DefaultMailUserAgent(
            final DeliveryRequestHandler handler,
            final IOReactorConfig config) throws IOException {
        super();
        if (handler == null) {
            throw new IllegalArgumentException("Delivery handler may not be null");
        }
        this.handler = handler;
        this.ioReactor = new DefaultConnectingIOReactor(config);
    }

    public void setExceptionHandler(final IOReactorExceptionHandler exceptionHandler) {
        this.ioReactor.setExceptionHandler(exceptionHandler);
    }

    protected IOEventDispatch createIOEventDispatch(
            final DeliveryRequestHandler handler) {
        return new ClientIOEventDispatch(handler);
    }

    private void execute() throws IOException {
        IOEventDispatch ioEventDispatch = createIOEventDispatch(this.handler);
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
