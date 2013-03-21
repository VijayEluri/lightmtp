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
package com.ok2c.lightmtp.agent;

import java.util.concurrent.Future;

import org.apache.http.concurrent.FutureCallback;

import com.ok2c.lightmtp.protocol.DeliveryRequest;
import com.ok2c.lightmtp.protocol.DeliveryResult;

public interface MailUserAgent extends MailTransport {

    /**
     * Start transport
     */
    void start();

    /**
     * Deliver mail
     *
     * @param remoteAddress
     * @param localAddress or null if the default should get used
     * @param request
     * @param callback
     * @return future
     */
    Future<DeliveryResult> deliver(
            SessionEndpoint endpoint,
            int connectTimeout,
            DeliveryRequest request,
            FutureCallback<DeliveryResult> callback);

}
