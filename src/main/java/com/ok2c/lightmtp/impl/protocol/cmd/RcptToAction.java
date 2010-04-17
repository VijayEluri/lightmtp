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

import com.ok2c.lightmtp.SMTPCode;
import com.ok2c.lightmtp.SMTPCodes;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.impl.protocol.ServerState;
import com.ok2c.lightmtp.protocol.EnvelopValidator;
import com.ok2c.lightnio.concurrent.FutureCallback;

class RcptToAction extends AbstractAsyncAction<ServerState> {

    private final String recipient;
    private final EnvelopValidator validator;

    public RcptToAction(final String recipient, final EnvelopValidator validator) {
        super();
        this.recipient = recipient;
        this.validator = validator;
    }

    @Override
    protected SMTPReply internalValidateState(final ServerState state) {
        if (state.getClientType() == null || state.getSender() == null) {
            SMTPReply reply = new SMTPReply(SMTPCodes.ERR_PERM_BAD_SEQUENCE,
                    new SMTPCode(5, 5, 1),
                    "bad sequence of commands");
            return reply;
        } else {
            return null;
        }
    }

    @Override
    protected Future<SMTPReply> internalAsyncExecute(
            final ServerState state,
            final FutureCallback<SMTPReply> callback) {
        return this.validator.validateRecipient(
                state.getClient(), this.recipient, callback);
    }

    @Override
    protected void internalUpdateState(final SMTPReply reply, final ServerState state) {
        if (reply.getCode() == SMTPCodes.OK) {
            state.getRecipients().add(this.recipient);
        }
    }

}
