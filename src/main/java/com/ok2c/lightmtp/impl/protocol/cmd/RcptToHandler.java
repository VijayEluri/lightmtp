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

import java.util.List;

import com.ok2c.lightmtp.SMTPCode;
import com.ok2c.lightmtp.SMTPCodes;
import com.ok2c.lightmtp.SMTPErrorException;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.impl.protocol.ServerSessionState;
import com.ok2c.lightmtp.protocol.Action;
import com.ok2c.lightmtp.protocol.CommandHandler;
import com.ok2c.lightmtp.protocol.EnvelopValidator;

public class RcptToHandler implements CommandHandler<ServerSessionState> {

    private final EnvelopValidator validator;
    private final AddressArgParser argParser;

    public RcptToHandler(final EnvelopValidator validator) {
        super();
        this.validator = validator;
        this.argParser = new AddressArgParser("TO:");
    }

    public Action<ServerSessionState> handle(
            final String argument, 
            final List<String> params,
            final ServerSessionState sessionState) throws SMTPErrorException {
        if (sessionState.getClientType() == null || sessionState.getSender() == null) {
            throw new SMTPErrorException(SMTPCodes.ERR_PERM_BAD_SEQUENCE, 
                    new SMTPCode(5, 5, 1),
                    "bad sequence of commands");
        }

        String recipient = this.argParser.parse(argument);
        
        if (this.validator != null) {
            this.validator.validateRecipient(recipient);
        }
        
        sessionState.getRecipients().add(recipient);
        return new SimpleAction(new SMTPReply(SMTPCodes.OK, new SMTPCode(2, 1, 5), 
                "recipient <" + recipient + "> ok"));
    }

}
