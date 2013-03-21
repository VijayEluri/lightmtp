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

import java.io.IOException;
import java.net.SocketAddress;

import org.apache.http.nio.pool.AbstractNIOConnPool;
import org.apache.http.nio.pool.NIOConnFactory;
import org.apache.http.nio.pool.SocketAddressResolver;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOSession;

import com.ok2c.lightmtp.agent.SessionEndpoint;

class CPool extends AbstractNIOConnPool<SessionEndpoint, IOSession, LeasedSession> {

    public CPool(
            final ConnectingIOReactor ioreactor, final int defaultMaxPerRoute, final int maxTotal) {
        super(ioreactor,
                new InternalConnFactory(),
                new InternalAddressResolver(),
                defaultMaxPerRoute, maxTotal);
    }

    @Override
    protected LeasedSession createEntry(final SessionEndpoint endpoint, final IOSession iosession) {
        return new LeasedSession(null, endpoint, iosession);
    }

    static class InternalConnFactory implements NIOConnFactory<SessionEndpoint, IOSession> {

        @Override
        public IOSession create(
                final SessionEndpoint endpoint, final IOSession iosession) throws IOException {
            return iosession;
        }

    }

    static class InternalAddressResolver implements SocketAddressResolver<SessionEndpoint> {

        @Override
        public SocketAddress resolveLocalAddress(final SessionEndpoint endpoint) throws IOException {
            return endpoint.getLocalAddress();
        }

        @Override
        public SocketAddress resolveRemoteAddress(final SessionEndpoint endpoint) throws IOException {
            return endpoint.getRemoteAddress();
        }

    }

}
