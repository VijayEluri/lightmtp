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
import com.ok2c.lightmtp.protocol.CommandHandler;
import com.ok2c.lightmtp.protocol.EnvelopValidator;

public class MailFromHandler implements CommandHandler<ServerSessionState> {

    private final EnvelopValidator validator;
    private final AddressArgParser argParser;

    public MailFromHandler(final EnvelopValidator validator) {
        super();
        this.validator = validator;
        this.argParser = new AddressArgParser("FROM:");
    }

    public SMTPReply handle(
            final String argument,
            final List<String> params,
            final ServerSessionState sessionState) {

        try {
            if (sessionState.getClientType() == null || sessionState.getSender() != null) {
                throw new SMTPErrorException(SMTPCodes.ERR_PERM_BAD_SEQUENCE, 
                        new SMTPCode(5, 5, 1),
                        "bad sequence of commands");
            }

            String sender = this.argParser.parse(argument);
            
            if (this.validator != null) {
                this.validator.validateSender(sender);
            }
            
            sessionState.setSender(sender);
            SMTPCode enhancedCode = null;
            if (sessionState.isEnhancedCodeCapable()) {
                enhancedCode = new SMTPCode(2, 1, 0);
            }
            return new SMTPReply(SMTPCodes.OK, enhancedCode, "originator <" + sender + "> ok");
        } catch (SMTPErrorException ex) {
            return new SMTPReply(ex.getCode(), 
                    sessionState.isEnhancedCodeCapable() ? ex.getEnhancedCode() : null, 
                    ex.getMessage());
        }

    }
    
}
