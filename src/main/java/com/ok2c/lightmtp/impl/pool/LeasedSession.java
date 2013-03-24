/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package com.ok2c.lightmtp.impl.pool;

import java.util.concurrent.TimeUnit;

import org.apache.http.nio.reactor.IOSession;
import org.apache.http.pool.PoolEntry;

import com.ok2c.lightmtp.agent.SessionEndpoint;

public class LeasedSession extends PoolEntry<SessionEndpoint, IOSession> {

    LeasedSession(
            final String id,
            final SessionEndpoint endpoint,
            final IOSession iosession) {
        super(id, endpoint, iosession, -1, TimeUnit.MILLISECONDS);
    }

    public IOSession getIOSession() {
        return getConnection();
    }

    public SessionEndpoint getSessionEndpoint() {
        return getRoute();
    }

    @Override
    public boolean isClosed() {
        return getConnection().isClosed();
    }

    @Override
    public void close() {
        getConnection().close();
    }

}
