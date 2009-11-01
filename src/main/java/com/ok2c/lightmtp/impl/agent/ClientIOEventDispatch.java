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

import com.ok2c.lightmtp.impl.protocol.ClientSession;
import com.ok2c.lightmtp.protocol.DeliveryRequestHandler;
import com.ok2c.lightnio.IOEventDispatch;
import com.ok2c.lightnio.IOSession;

public class ClientIOEventDispatch implements IOEventDispatch {

    private static final String CLIENT_SESSION = "smtp.client-session";
    
    private final DeliveryRequestHandler handler;
    
    public ClientIOEventDispatch(final DeliveryRequestHandler handler) {
        super();
        if (handler == null) {
            throw new IllegalArgumentException("Delivery request handler may not be null");
        }
        this.handler = handler;
    }
    
    public void connected(final IOSession iosession) {
        ClientSession clientSession = new ClientSession(iosession, this.handler); 
        iosession.setAttribute(CLIENT_SESSION, clientSession);
        clientSession.connected();
    }

    public void disconnected(final IOSession iosession) {
        ClientSession clientSession = (ClientSession) iosession.getAttribute(CLIENT_SESSION);
        if (clientSession != null) {
            clientSession.disconneced();
        }
    }

    public void inputReady(final IOSession iosession) {
        ClientSession clientSession = (ClientSession) iosession.getAttribute(CLIENT_SESSION);
        clientSession.consumeData();
    }

    public void outputReady(final IOSession iosession) {
        ClientSession clientSession = (ClientSession) iosession.getAttribute(CLIENT_SESSION);
        clientSession.produceData();
    }

    public void timeout(final IOSession iosession) {
        ClientSession clientSession = (ClientSession) iosession.getAttribute(CLIENT_SESSION);
        clientSession.timeout();
    }

}
