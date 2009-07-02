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
package com.ok2c.lightmtp.protocol.impl;

import com.ok2c.lightmtp.protocol.SessionContext;
import com.ok2c.lightnio.IOSession;

class SessionContextImpl implements SessionContext {

    private final IOSession iosession;
    
    public SessionContextImpl(final IOSession iosession) {
        super();
        this.iosession = iosession;
    }
    
    public void setAttribute(final String name, final Object obj) {
        this.iosession.setAttribute(name, obj);
    }
    
    public Object getAttribute(final String name) {
        return this.iosession.getAttribute(name);
    }

    public Object removeAttribute(String name) {
        return this.iosession.removeAttribute(name);
    }

}
