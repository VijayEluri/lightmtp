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

import java.io.IOException;

import org.apache.http.impl.nio.reactor.IOReactorConfig;

import com.ok2c.lightmtp.impl.protocol.LocalClientSessionFactory;
import com.ok2c.lightmtp.protocol.DeliveryRequestHandler;

public class LocalMailClientTransport extends DefaultMailClientTransport {

    public LocalMailClientTransport(
            final IOSessionRegistryCallback sessionRegistryCallback,
            final IOReactorThreadCallback reactorThreadCallback,
            final IOReactorConfig config) throws IOException {
        super(sessionRegistryCallback, reactorThreadCallback, config);
    }

    public LocalMailClientTransport(
            final IOReactorConfig config) throws IOException {
        this(null, null, config);
    }

    @Override
    public void start(final DeliveryRequestHandler deliveryRequestHandler) {
        LocalClientSessionFactory sessionFactory = new LocalClientSessionFactory(
                deliveryRequestHandler);
        start(sessionFactory);
    }

}
