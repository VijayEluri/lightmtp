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
package com.ok2c.lightmtp.impl.protocol.cmd;

import java.util.concurrent.Future;

import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.protocol.Action;
import com.ok2c.lightnio.concurrent.BasicFuture;
import com.ok2c.lightnio.concurrent.FutureCallback;

abstract class AbstractAsyncAction<T> implements Action<T> {

    public AbstractAsyncAction() {
        super();
    }

    protected abstract SMTPReply internalValidateState(T state);

    protected abstract Future<SMTPReply> internalAsyncExecute(T state,
            FutureCallback<SMTPReply> callback);

    protected abstract void internalUpdateState(SMTPReply reply, T state);

    public Future<SMTPReply> execute(
            final T state,
            final FutureCallback<SMTPReply> callback) {
        synchronized (state) {
            SMTPReply reply = internalValidateState(state);
            if (reply != null) {
                BasicFuture<SMTPReply> future = new BasicFuture<SMTPReply>(callback);
                future.completed(reply);
                return future;
            } else {
                return internalAsyncExecute(state, new InternalCallback(state, callback));
            }
        }
    }

    class InternalCallback implements FutureCallback<SMTPReply> {

        private final T state;
        private final FutureCallback<SMTPReply> callback;

        InternalCallback(
                final T state,
                final FutureCallback<SMTPReply> callback) {
            this.state = state;
            this.callback = callback;
        }

        public void completed(final SMTPReply reply) {
            synchronized (this.state) {
                internalUpdateState(reply, this.state);
            }
            if (this.callback != null) {
                this.callback.completed(reply);
            }
        }

        public void cancelled() {
            if (this.callback != null) {
                this.callback.cancelled();
            }
        }

        public void failed(final Exception ex) {
            if (this.callback != null) {
                this.callback.failed(ex);
            }
        }

    }

}
