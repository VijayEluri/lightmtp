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

import com.ok2c.lightmtp.protocol.DeliveryRequestHandler;
import com.ok2c.lightmtp.protocol.ProtocolCodecs;
import com.ok2c.lightmtp.protocol.SessionFactory;
import com.ok2c.lightnio.IOSession;

public class LocalClientSessionFactory implements SessionFactory<ClientSession> {

    private final DeliveryRequestHandler deliveryRequestHandler;
    
    public LocalClientSessionFactory(
            final DeliveryRequestHandler deliveryRequestHandler) {
        super();
        if (deliveryRequestHandler == null) {
            throw new IllegalArgumentException("Delivery request handler may not be null");
        }
        this.deliveryRequestHandler = deliveryRequestHandler;
    }
    
    public ClientSession create(final IOSession iosession) {
        ProtocolCodecs<ClientSessionState> codecs = new ProtocolCodecRegistry<ClientSessionState>();        
        codecs.register(ProtocolState.HELO.name(), new SendLocalHeloCodec());
        codecs.register(ProtocolState.MAIL.name(), new PipeliningSendEnvelopCodec(true));
        codecs.register(ProtocolState.DATA.name(), new SendDataCodec(true, DataAckMode.PER_RECIPIENT));
        codecs.register(ProtocolState.QUIT.name(), new SendQuitCodec());
        codecs.register(ProtocolState.RSET.name(), new SendRsetCodec());
        return new ClientSession(iosession, this.deliveryRequestHandler, codecs);
    }

}