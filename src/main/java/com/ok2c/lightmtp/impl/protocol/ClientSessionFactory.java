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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ok2c.lightmtp.protocol.DeliveryRequestHandler;
import com.ok2c.lightmtp.protocol.ProtocolCodecs;
import com.ok2c.lightmtp.protocol.SessionFactory;
import com.ok2c.lightnio.IOSession;

public class ClientSessionFactory implements SessionFactory<ClientSession> {

    private final Logger log = LoggerFactory.getLogger(ServerSession.class);
    private final Logger iolog = LoggerFactory.getLogger(IOSession.class);
    private final Logger wirelog = LoggerFactory.getLogger(Wire.WIRELOG_CAT);

    private final DeliveryRequestHandler deliveryRequestHandler;
	private final String heloName;
    private String username;
    private String password;

    public ClientSessionFactory(
            final DeliveryRequestHandler deliveryRequestHandler) {
        this(deliveryRequestHandler, null, null, null);
    }


    public ClientSessionFactory(
            final DeliveryRequestHandler deliveryRequestHandler, String heloName, String username, String password) {
        super();
        if (deliveryRequestHandler == null) {
            throw new IllegalArgumentException("Delivery request handler may not be null");
        }
        this.deliveryRequestHandler = deliveryRequestHandler;
        this.heloName = heloName;
        this.username = username;
        this.password = password;
    }
    
    public ClientSession create(final IOSession iosession) {
        SMTPBuffers iobuffers = new SMTPBuffers();
        ProtocolCodecs<ClientState> codecs = new ProtocolCodecRegistry<ClientState>();
        codecs.register(ProtocolState.HELO.name(), new ExtendedSendHeloCodec(iobuffers, heloName));
        if (username != null && password != null) {
            codecs.register(ProtocolState.AUTH.name(), new AuthCodec(iobuffers, username, password));
        }
        codecs.register(ProtocolState.MAIL.name(), new SimpleSendEnvelopCodec(iobuffers, false));
        codecs.register(ProtocolState.DATA.name(), new SendDataCodec(iobuffers, false));
        codecs.register(ProtocolState.QUIT.name(), new SendQuitCodec(iobuffers));
        codecs.register(ProtocolState.RSET.name(), new SendRsetCodec(iobuffers));
        return new ClientSession(this.log, this.iolog, this.wirelog,
                iosession, iobuffers, this.deliveryRequestHandler, codecs);
    }

}
