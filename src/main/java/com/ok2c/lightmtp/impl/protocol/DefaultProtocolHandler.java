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
package com.ok2c.lightmtp.impl.protocol;

import java.util.ArrayList;
import java.util.List;

import com.ok2c.lightmtp.SMTPCodes;
import com.ok2c.lightmtp.SMTPCommand;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.protocol.ProtocolHandler;

public class DefaultProtocolHandler implements ProtocolHandler<ServerSessionState> {

    public DefaultProtocolHandler() {
        super();
    }

    public SMTPReply handle(final SMTPCommand command, final ServerSessionState sessionState) {
        String cmd = command.getCode();
        String domain = command.getArgument();
        if (cmd.equalsIgnoreCase("HELO")) {
            if (domain != null) {
                sessionState.setClientType(ClientType.BASIC);
                sessionState.setClientDomain(domain);
                return new SMTPReply(SMTPCodes.OK, "Welcome " + domain);
            } else {
                return new SMTPReply(SMTPCodes.ERR_PERM_SYNTAX_ERR_COMMAND, "domain not given");
            }
        } else if (cmd.equalsIgnoreCase("EHLO")) {
            if (domain != null) {
                sessionState.setClientType(ClientType.EXTENDED);
                sessionState.setClientDomain(domain);

                List<String> lines = new ArrayList<String>();
                lines.add("Welcome " + domain);
                lines.addAll(sessionState.getExtensions());
                return new SMTPReply(SMTPCodes.OK, null, lines);
            } else {
                return new SMTPReply(SMTPCodes.ERR_PERM_SYNTAX_ERR_COMMAND, "domain not given");
            }
        } else if (cmd.equalsIgnoreCase("NOOP")) {
            return new SMTPReply(SMTPCodes.OK, "OK");
        } else if (cmd.equalsIgnoreCase("RSET")) {
            sessionState.reset();
            return new SMTPReply(SMTPCodes.OK, "OK");
        } else if (cmd.equalsIgnoreCase("QUIT")) {
            return new SMTPReply(SMTPCodes.SERVICE_TERMINATING,
                    sessionState.getServerId() + " service terminating");
        } else {
            return new SMTPReply(SMTPCodes.ERR_PERM_SYNTAX_ERR_COMMAND, "command not recognized");
        }
    }

}
