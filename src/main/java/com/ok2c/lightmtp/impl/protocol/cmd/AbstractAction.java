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

public abstract class AbstractAction<T> implements Action<T> {

    public AbstractAction() {
        super();
    }

    public Future<SMTPReply> execute(
            final T state,
            final FutureCallback<SMTPReply> callback) {
        synchronized (state) {
            BasicFuture<SMTPReply> future = new BasicFuture<SMTPReply>(callback);
            future.completed(internalExecute(state));
            return future;
        }
    }

    protected abstract SMTPReply internalExecute(T state);

}
