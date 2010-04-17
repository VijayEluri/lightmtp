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

import com.ok2c.lightmtp.SMTPCodes;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.impl.protocol.ClientType;
import com.ok2c.lightmtp.impl.protocol.ServerState;

public class HeloAction extends AbstractAction<ServerState> {

    private final String domain;

    public HeloAction(final String domain) {
        super();
        this.domain = domain;
    }

    @Override
    protected SMTPReply internalExecute(final ServerState state) {
        state.reset();
        state.setClientType(ClientType.BASIC);
        state.setClientDomain(this.domain);
        return new SMTPReply(SMTPCodes.OK, null, "Welcome " + this.domain);
    }

}
