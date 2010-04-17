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

import java.io.IOException;
import java.util.List;

import com.ok2c.lightnio.IOReactorStatus;
import com.ok2c.lightnio.impl.ExceptionEvent;

public interface MailTransport {

    IOReactorStatus getStatus();

    Exception getException();

    List<ExceptionEvent> getAuditLog();

    void shutdown() throws IOException;

    void forceShutdown();

}
