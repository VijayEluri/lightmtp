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

import com.ok2c.lightmtp.impl.protocol.ServerSession;
import com.ok2c.lightmtp.protocol.SessionFactory;
import com.ok2c.lightnio.IOEventDispatch;
import com.ok2c.lightnio.IOSession;

public class ServerIOEventDispatch implements IOEventDispatch {

    private static final String SERVER_SESSION = "smtp.server-session";

    private final IOSessionRegistry sessionRegistry;
    private final SessionFactory<ServerSession> sessionFactory;

    public ServerIOEventDispatch(
            final IOSessionRegistry sessionRegistry,
            final SessionFactory<ServerSession> sessionFactory) {
        super();
        if (sessionRegistry == null) {
            throw new IllegalArgumentException("I/O session registry may not be null");
        }
        if (sessionFactory == null) {
            throw new IllegalArgumentException("Session factory may not be null");
        }
        this.sessionRegistry = sessionRegistry;
        this.sessionFactory = sessionFactory;
    }
    
    public void connected(final IOSession iosession) {
        ServerSession serverSession = this.sessionFactory.create(iosession);
        iosession.setAttribute(SERVER_SESSION, serverSession);
        serverSession.connected();
        this.sessionRegistry.add(iosession);
    }

    public void disconnected(final IOSession iosession) {
        ServerSession serverSession = (ServerSession) iosession.getAttribute(SERVER_SESSION);
        if (serverSession != null) {
            serverSession.disconneced();
        }
        this.sessionRegistry.remove(iosession);
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
