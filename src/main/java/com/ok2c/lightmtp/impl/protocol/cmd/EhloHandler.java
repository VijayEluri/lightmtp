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

import java.util.ArrayList;
import java.util.List;

import com.ok2c.lightmtp.SMTPCode;
import com.ok2c.lightmtp.SMTPCodes;
import com.ok2c.lightmtp.SMTPErrorException;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.impl.protocol.ClientType;
import com.ok2c.lightmtp.impl.protocol.ServerState;
import com.ok2c.lightmtp.protocol.Action;
import com.ok2c.lightmtp.protocol.CommandHandler;
import com.ok2c.lightmtp.protocol.EnvelopValidator;

public class EhloHandler implements CommandHandler<ServerState> {

    private final EnvelopValidator validator;
    
    public EhloHandler(final EnvelopValidator validator) {
        super();
        this.validator = validator;
    }

    public Action<SMTPReply> handle(
            final String argument, 
            final List<String> params,
            final ServerState sessionState) throws SMTPErrorException {
        // Reset session
        sessionState.reset();
        String domain = argument;
        if (domain == null) {
            throw new SMTPErrorException(SMTPCodes.ERR_PERM_SYNTAX_ERR_COMMAND, 
                    new SMTPCode(5, 5, 2),
                    "domain not given");
        }
        
        if (this.validator != null) {
            this.validator.validateClientDomain(domain);
        }
        
        sessionState.setClientType(ClientType.EXTENDED);
        sessionState.setClientDomain(domain);

        List<String> lines = new ArrayList<String>();
        lines.add("Welcome " + domain);
        lines.addAll(sessionState.getExtensions());
        return new SimpleAction(new SMTPReply(SMTPCodes.OK, null, lines));
    }

}
