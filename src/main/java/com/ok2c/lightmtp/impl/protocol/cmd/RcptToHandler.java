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
import com.ok2c.lightmtp.SMTPProtocolException;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.impl.protocol.ServerSessionState;
import com.ok2c.lightmtp.protocol.CommandHandler;

public class RcptToHandler implements CommandHandler<ServerSessionState> {

    private final AddressArgParser argParser;

    public RcptToHandler() {
        super();
        this.argParser = new AddressArgParser("TO:");
    }

    public SMTPReply handle(
            final String argument,
            final List<String> params,
            final ServerSessionState sessionState) {

        if (sessionState.getClientType() != null && sessionState.getSender() != null) {
            try {
                String recipient = this.argParser.parse(argument);
                sessionState.getRecipients().add(recipient);
                SMTPCode enhancedCode = null;
                if (sessionState.isEnhancedCodeCapable()) {
                    enhancedCode = new SMTPCode(2, 1, 5);
                }
                return new SMTPReply(SMTPCodes.OK, enhancedCode, "recipient <" + recipient + "> ok");
            } catch (SMTPProtocolException ex) {
                SMTPCode enhancedCode = null;
                if (sessionState.isEnhancedCodeCapable()) {
                    enhancedCode = new SMTPCode(5, 5, 1);
                }
                return new SMTPReply(SMTPCodes.ERR_PERM_SYNTAX_ERR_COMMAND, enhancedCode,
                        ex.getMessage());
            }
        } else {
            SMTPCode enhancedCode = null;
            if (sessionState.isEnhancedCodeCapable()) {
                enhancedCode = new SMTPCode(5, 5, 1);
            }
            return new SMTPReply(SMTPCodes.ERR_PERM_BAD_SEQUENCE, enhancedCode,
                    "bad sequence of commands");
        }
    }

}
