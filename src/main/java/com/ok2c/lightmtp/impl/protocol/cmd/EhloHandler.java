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
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.impl.protocol.ClientType;
import com.ok2c.lightmtp.impl.protocol.ServerSessionState;
import com.ok2c.lightmtp.protocol.CommandHandler;

public class EhloHandler implements CommandHandler<ServerSessionState> {

    public EhloHandler() {
        super();
    }

    public SMTPReply handle(
            final String argument,
            final List<String> params,
            final ServerSessionState sessionState) {

        // Reset session
        sessionState.reset();

        String domain = argument;
        if (domain != null) {
            sessionState.setClientType(ClientType.EXTENDED);
            sessionState.setClientDomain(domain);

            List<String> lines = new ArrayList<String>();
            lines.add("Welcome " + domain);
            lines.addAll(sessionState.getExtensions());
            return new SMTPReply(SMTPCodes.OK, null, lines);
        } else {
            sessionState.setClientType(null);
            sessionState.setClientDomain(null);
            return new SMTPReply(SMTPCodes.ERR_PERM_SYNTAX_ERR_COMMAND, new SMTPCode(5, 5, 2),
                    "domain not given");
        }
    }

}
