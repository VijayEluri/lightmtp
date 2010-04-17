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
package com.ok2c.lightmtp.impl.protocol;

import java.nio.channels.SelectionKey;

import com.ok2c.lightnio.IOSession;
import com.ok2c.lightnio.concurrent.FutureCallback;

class OutputTrigger<T> implements FutureCallback<T>{

    final ServerState state;
    final IOSession iosession;

    public OutputTrigger(final ServerState state, final IOSession iosession) {
        super();
        this.iosession = iosession;
        this.state = state;
    }

    private void resume() {
        synchronized (this.state) {
            this.iosession.setEvent(SelectionKey.OP_WRITE);
        }
    }

    public void cancelled() {
        resume();
    }

    public void completed(final T result) {
        resume();
    }

    public void failed(final Exception ex) {
        resume();
    }

}
