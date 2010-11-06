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

public class LocalClientSessionFactory implements SessionFactory<ClientSession> {

    private final Logger log = LoggerFactory.getLogger(ServerSession.class);
    private final Logger iolog = LoggerFactory.getLogger(IOSession.class);
    private final Logger wirelog = LoggerFactory.getLogger(Wire.WIRELOG_CAT);

    private final DeliveryRequestHandler deliveryRequestHandler;
	private final String heloName;

    public LocalClientSessionFactory(
            final DeliveryRequestHandler deliveryRequestHandler, String heloName) {
        super();
        if (deliveryRequestHandler == null) {
            throw new IllegalArgumentException("Delivery request handler may not be null");
        }
        this.deliveryRequestHandler = deliveryRequestHandler;
        this.heloName = heloName;
    }
    public LocalClientSessionFactory(
            final DeliveryRequestHandler deliveryRequestHandler) {
        this(deliveryRequestHandler, null);
    }

    public ClientSession create(final IOSession iosession) {
        SMTPBuffers iobuffers = new SMTPBuffers();
        ProtocolCodecs<ClientState> codecs = new ProtocolCodecRegistry<ClientState>();
        codecs.register(ProtocolState.HELO.name(),
                new SendLocalHeloCodec(iobuffers, heloName));
        codecs.register(ProtocolState.MAIL.name(),
                new PipeliningSendEnvelopCodec(iobuffers, true));
        codecs.register(ProtocolState.DATA.name(),
                new SendDataCodec(iobuffers, true, DataAckMode.PER_RECIPIENT));
        codecs.register(ProtocolState.QUIT.name(),
                new SendQuitCodec(iobuffers));
        codecs.register(ProtocolState.RSET.name(),
                new SendRsetCodec(iobuffers));
        return new ClientSession(this.log, this.iolog, this.wirelog,
                iosession, iobuffers, this.deliveryRequestHandler, codecs);
    }

}
