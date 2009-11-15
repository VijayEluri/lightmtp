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

import com.ok2c.lightmtp.SMTPCodes;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.impl.protocol.ServerState;
import com.ok2c.lightmtp.protocol.Action;
import com.ok2c.lightmtp.protocol.EnvelopValidator;
import com.ok2c.lightnio.concurrent.FutureCallback;

class CheckRecipientAction implements Action<SMTPReply> {

    private final String recipient;
    private final ServerState state;
    private final EnvelopValidator validator;
    
    public CheckRecipientAction(
            final String recipient,
            final ServerState state, 
            final EnvelopValidator validator) {
        super();
        this.recipient = recipient;
        this.state = state;
        this.validator = validator;
    }

    public Future<SMTPReply> execute(final FutureCallback<SMTPReply> callback) {
        return this.validator.validateRecipient(this.recipient, new InternalCallback(callback));
    }
    
    class InternalCallback implements FutureCallback<SMTPReply> {

        private final FutureCallback<SMTPReply> callback;
        
        InternalCallback(final FutureCallback<SMTPReply> callback) {
            this.callback = callback;
        }

        public void completed(final SMTPReply reply) {
            synchronized (state) {
                if (reply.getCode() == SMTPCodes.OK) {
                    state.getRecipients().add(recipient);
                }
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
