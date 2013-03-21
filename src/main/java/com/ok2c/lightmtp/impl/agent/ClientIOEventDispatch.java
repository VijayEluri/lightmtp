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

import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.util.Args;

import com.ok2c.lightmtp.impl.protocol.ClientSession;
import com.ok2c.lightmtp.protocol.SessionFactory;

public class ClientIOEventDispatch implements IOEventDispatch {

    private static final String CLIENT_SESSION = "smtp.client-session";

    private final IOSessionRegistry sessionRegistry;
    private final SessionFactory<ClientSession> sessionFactory;

    public ClientIOEventDispatch(
            final IOSessionRegistry sessionRegistry,
            final SessionFactory<ClientSession> sessionFactory) {
        super();
        Args.notNull(sessionRegistry, "I/O session registry");
        Args.notNull(sessionFactory, "Session factory");
        this.sessionRegistry = sessionRegistry;
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void connected(final IOSession iosession) {
        ClientSession clientSession = this.sessionFactory.create(iosession);
        iosession.setAttribute(CLIENT_SESSION, clientSession);
        clientSession.connected();
        this.sessionRegistry.add(iosession);
    }

    @Override
    public void disconnected(final IOSession iosession) {
        ClientSession clientSession = (ClientSession) iosession.getAttribute(CLIENT_SESSION);
        if (clientSession != null) {
            clientSession.disconneced();
        }
        this.sessionRegistry.remove(iosession);
    }

    @Override
    public void inputReady(final IOSession iosession) {
        ClientSession clientSession = (ClientSession) iosession.getAttribute(CLIENT_SESSION);
        clientSession.consumeData();
    }

    @Override
    public void outputReady(final IOSession iosession) {
        ClientSession clientSession = (ClientSession) iosession.getAttribute(CLIENT_SESSION);
        clientSession.produceData();
    }

    @Override
    public void timeout(final IOSession iosession) {
        ClientSession clientSession = (ClientSession) iosession.getAttribute(CLIENT_SESSION);
        clientSession.timeout();
    }

}
