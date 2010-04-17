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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ok2c.lightnio.IOEventDispatch;
import com.ok2c.lightnio.IOReactor;

class IOReactorThread extends Thread {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final IOReactor ioReactor;
    private final IOEventDispatch iodispatch;
    private final IOReactorThreadCallback callback;

    private volatile Exception ex;

    public IOReactorThread(
            final IOReactor ioReactor,
            final IOEventDispatch iodispatch,
            final IOReactorThreadCallback callback) {
        super();
        this.ioReactor = ioReactor;
        this.iodispatch = iodispatch;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            this.ioReactor.execute(this.iodispatch);
            if (this.callback != null) {
                this.callback.terminated();
            }
        } catch (Exception ex) {
            this.ex = ex;
            this.log.error("I/O reactor terminated abnormally", ex);
            if (this.callback != null) {
                this.callback.terminated(ex);
            }
        }
    }

    public Exception getException() {
        return this.ex;
    }

}
