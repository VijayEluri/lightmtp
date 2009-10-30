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

import com.ok2c.lightmtp.SMTPCodes;
import com.ok2c.lightmtp.SMTPErrorException;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.impl.protocol.ClientType;
import com.ok2c.lightmtp.impl.protocol.ServerSessionState;
import com.ok2c.lightmtp.protocol.CommandHandler;
import com.ok2c.lightmtp.protocol.EnvelopValidator;

public class HeloHandler implements CommandHandler<ServerSessionState> {

    private final EnvelopValidator validator;
    
    public HeloHandler(final EnvelopValidator validator) {
        super();
        this.validator = validator;
    }

    public SMTPReply handle(
            final String argument,
            final List<String> params,
            final ServerSessionState sessionState) {

        // Reset session
        sessionState.reset();

        try {
            String domain = argument;
            if (domain == null) {
                throw new SMTPErrorException(SMTPCodes.ERR_PERM_SYNTAX_ERR_COMMAND, 
                        null,
                        "domain not given");
            }
            
            if (this.validator != null) {
                this.validator.validateClientDomain(domain);
            }
            sessionState.setClientType(ClientType.BASIC);
            sessionState.setClientDomain(domain);
            return new SMTPReply(SMTPCodes.OK, "Welcome " + domain);
        } catch (SMTPErrorException ex) {
            sessionState.setClientType(null);
            sessionState.setClientDomain(null);
            return new SMTPReply(ex.getCode(), ex.getMessage());
        }
    }

}
