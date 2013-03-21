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

import org.apache.http.nio.reactor.IOSession;
import org.apache.http.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ok2c.lightmtp.protocol.DeliveryRequestHandler;
import com.ok2c.lightmtp.protocol.ProtocolCodecs;
import com.ok2c.lightmtp.protocol.SessionFactory;

public class ClientSessionFactory implements SessionFactory<ClientSession> {

    private final Logger iolog = LoggerFactory.getLogger(IOSession.class);
    private final Logger wirelog = LoggerFactory.getLogger(Wire.WIRELOG_CAT);

    private final DeliveryRequestHandler deliveryRequestHandler;
	private final String heloName;
    private final String username;
    private final String password;

    public ClientSessionFactory(
            final DeliveryRequestHandler deliveryRequestHandler) {
        this(deliveryRequestHandler, null, null, null);
    }


    public ClientSessionFactory(
            final DeliveryRequestHandler deliveryRequestHandler, final String heloName, final String username, final String password) {
        super();
        Args.notNull(deliveryRequestHandler, "Delivery request handler");
        this.deliveryRequestHandler = deliveryRequestHandler;
        this.heloName = heloName;
        this.username = username;
        this.password = password;
    }

    @Override
    public ClientSession create(final IOSession iosession) {
        SMTPBuffers iobuffers = new SMTPBuffers();
        ProtocolCodecs<ClientState> codecs = new ProtocolCodecRegistry<ClientState>();
        codecs.register(ProtocolState.HELO.name(), new ExtendedSendHeloCodec(iobuffers, heloName, (username != null && password != null)));
        if (username != null && password != null) {
            codecs.register(ProtocolState.AUTH.name(), new AuthCodec(iobuffers, username, password));
        }
        codecs.register(ProtocolState.MAIL.name(), new SimpleSendEnvelopCodec(iobuffers, false));
        codecs.register(ProtocolState.DATA.name(), new SendDataCodec(iobuffers, false));
        codecs.register(ProtocolState.QUIT.name(), new SendQuitCodec(iobuffers));
        codecs.register(ProtocolState.RSET.name(), new SendRsetCodec(iobuffers));
        final IOSession localIOSession;
        if (iolog.isDebugEnabled() || wirelog.isDebugEnabled()) {
            localIOSession = new LoggingIOSession(iosession, "SMTP client", iolog, wirelog);
        } else {
            localIOSession = iosession;
        }
        return new ClientSession(localIOSession, iobuffers, this.deliveryRequestHandler, codecs);
    }

}
