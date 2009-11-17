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

import com.ok2c.lightmtp.SMTPCode;
import com.ok2c.lightmtp.SMTPCodes;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.impl.protocol.DataType;
import com.ok2c.lightmtp.impl.protocol.ServerState;

public class DataAction extends AbstractAction<ServerState> {

    public DataAction() {
        super();
    }

    @Override
    protected SMTPReply internalExecute(final ServerState state) {
        if (state.getClientType() == null || state.getSender() == null) {
            return new SMTPReply(SMTPCodes.ERR_PERM_BAD_SEQUENCE, 
                    new SMTPCode(5, 5, 1),
                    "bad sequence of commands");
        } else if (state.getRecipients().isEmpty()) {
            return new SMTPReply(SMTPCodes.ERR_PERM_BAD_SEQUENCE, 
                    new SMTPCode(5, 5, 1),
                    "no valid recipients");
        } else {
            state.setDataType(DataType.ASCII);
            return new SMTPReply(SMTPCodes.START_MAIL_INPUT, 
                    null, 
                    "send message, ending in <CRLF>.<CRLF>");
        }
    }

}
