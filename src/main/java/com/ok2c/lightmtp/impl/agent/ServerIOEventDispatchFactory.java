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

import com.ok2c.lightmtp.agent.IOEventDispatchFactory;
import com.ok2c.lightmtp.protocol.DeliveryHandler;
import com.ok2c.lightmtp.protocol.EnvelopValidator;
import com.ok2c.lightnio.IOEventDispatch;

public class ServerIOEventDispatchFactory implements IOEventDispatchFactory {

    private final File workingDir;
    private final EnvelopValidator envelopValidator;
    private final DeliveryHandler deliveryHandler;

    public ServerIOEventDispatchFactory(
            final File workingDir,
            final EnvelopValidator envelopValidator,
            final DeliveryHandler deliveryHandler) {
        super();
        if (workingDir == null) {
            throw new IllegalArgumentException("Working dir may not be null");
        }
        if (deliveryHandler == null) {
            throw new IllegalArgumentException("Delivery handler may not be null");
        }
        this.workingDir = workingDir;
        this.envelopValidator = envelopValidator;
        this.deliveryHandler = deliveryHandler;
    }

    public IOEventDispatch createIOEventDispatch() {
        return new ServerIOEventDispatch(
                this.workingDir, 
                this.envelopValidator,
                this.deliveryHandler);
    }

}
