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
package com.ok2c.lightmtp.impl.agent;

import java.io.File;

import com.ok2c.lightmtp.impl.protocol.ServerSession;
import com.ok2c.lightmtp.protocol.DeliveryHandler;
import com.ok2c.lightmtp.protocol.EnvelopValidator;
import com.ok2c.lightnio.IOEventDispatch;
import com.ok2c.lightnio.IOSession;

public class ServerIOEventDispatch implements IOEventDispatch {

    private static final String SERVER_SESSION = "smtp.server-session";

    private final String serverId;
    private final File workingDir;
    private final EnvelopValidator validator;
    private final DeliveryHandler handler;

    public ServerIOEventDispatch(
            final String serverId,
            final File workingDir,
            final EnvelopValidator validator,
            final DeliveryHandler handler) {
        super();
        if (workingDir == null) {
            throw new IllegalArgumentException("Working dir may not be null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Delivery handler may not be null");
        }
        this.serverId = serverId;
        this.workingDir = workingDir;
        this.validator = validator;
        this.handler = handler;
    }

    public void connected(final IOSession iosession) {
        ServerSession serverSession = new ServerSession(
                iosession,
                this.serverId,
                this.workingDir,
                this.validator,
                this.handler);
        iosession.setAttribute(SERVER_SESSION, serverSession);
        serverSession.connected();
    }

    public void disconnected(final IOSession iosession) {
        ServerSession serverSession = (ServerSession) iosession.getAttribute(SERVER_SESSION);
        if (serverSession != null) {
            serverSession.disconneced();
        }
    }

    public void inputReady(final IOSession iosession) {
        ServerSession serverSession = (ServerSession) iosession.getAttribute(SERVER_SESSION);
        serverSession.consumeData();
    }

    public void outputReady(final IOSession iosession) {
        ServerSession serverSession = (ServerSession) iosession.getAttribute(SERVER_SESSION);
        serverSession.produceData();
    }

    public void timeout(final IOSession iosession) {
        ServerSession serverSession = (ServerSession) iosession.getAttribute(SERVER_SESSION);
        serverSession.timeout();
    }

}
