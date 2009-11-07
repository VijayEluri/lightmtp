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
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.impl.protocol.ServerSessionState;
import com.ok2c.lightmtp.protocol.CommandHandler;

public class QuitHandler implements CommandHandler<ServerSessionState> {

    public QuitHandler() {
        super();
    }

    public SMTPReply handle(
            final String argument,
            final List<String> params,
            final ServerSessionState sessionState) {
        sessionState.terminated();
        return new SMTPReply(SMTPCodes.SERVICE_TERMINATING,
                sessionState.getServerId() + " service terminating");
    }

}
