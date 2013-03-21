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

import org.apache.http.concurrent.FutureCallback;

import com.ok2c.lightmtp.SMTPCode;
import com.ok2c.lightmtp.SMTPCodes;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.impl.protocol.MIMEEncoding;
import com.ok2c.lightmtp.impl.protocol.ServerState;
import com.ok2c.lightmtp.protocol.EnvelopValidator;
import com.ok2c.lightmtp.protocol.UniqueIdGenerator;

class MailFromAction extends AbstractAsyncAction<ServerState> {

    private final String sender;
    private final MIMEEncoding mimeEncoding;
    private final EnvelopValidator validator;
    private final UniqueIdGenerator idgenerator;

    public MailFromAction(
            final String sender,
            final MIMEEncoding mimeEncoding,
            final EnvelopValidator validator,
            final UniqueIdGenerator idgenerator) {
        super();
        this.sender = sender;
        this.mimeEncoding = mimeEncoding;
        this.validator = validator;
        this.idgenerator = idgenerator;
    }

    @Override
    protected SMTPReply internalValidateState(final ServerState state) {
        if (state.getClientType() == null || state.getSender() != null) {
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
        state.setMessageId(this.idgenerator.generate());
        return this.validator.validateSender(state.getClient(), this.sender, callback);
    }

    @Override
    protected void internalUpdateState(final SMTPReply reply, final ServerState state) {
        if (reply.getCode() == SMTPCodes.OK) {
            state.setMimeEncoding(this.mimeEncoding);
            state.setSender(this.sender);
        }
    }

}
