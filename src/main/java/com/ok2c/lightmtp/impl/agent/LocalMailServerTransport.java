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
import java.io.IOException;

import com.ok2c.lightmtp.impl.protocol.LocalServerSessionFactory;
import com.ok2c.lightmtp.protocol.DeliveryHandler;
import com.ok2c.lightmtp.protocol.EnvelopValidator;
import com.ok2c.lightmtp.protocol.RemoteAddressValidator;
import com.ok2c.lightmtp.protocol.UniqueIdGenerator;
import com.ok2c.lightnio.impl.IOReactorConfig;

public class LocalMailServerTransport extends DefaultMailServerTransport {

    public LocalMailServerTransport(
            final IOSessionRegistryCallback sessionRegistryCallback,
            final IOReactorThreadCallback reactorThreadCallback,
            final File workingDir,
            final IOReactorConfig config) throws IOException {
        super(sessionRegistryCallback, reactorThreadCallback, workingDir, config);
    }

    public LocalMailServerTransport(
            final File workingDir,
            final IOReactorConfig config) throws IOException {
        this(null, null, workingDir, config);
    }

    public void start(
            final UniqueIdGenerator idgenerator,
            final RemoteAddressValidator addressValidator,
            final EnvelopValidator envelopValidator,
            final DeliveryHandler deliveryHandler) {
        LocalServerSessionFactory sessionFactory = new LocalServerSessionFactory(
                getWorkingDir(),
                idgenerator,
                addressValidator,
                envelopValidator,
                deliveryHandler);
        start(sessionFactory);
    }

    public void start(
            final UniqueIdGenerator idgenerator,
            final EnvelopValidator envelopValidator,
            final DeliveryHandler deliveryHandler) {
        start(idgenerator, null, envelopValidator, deliveryHandler);
    }

}
