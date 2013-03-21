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
package com.ok2c.lightmtp.agent;

import java.net.SocketAddress;

import org.apache.http.util.Args;
import org.apache.http.util.LangUtils;

public final class SessionEndpoint {

    private final SocketAddress localAddress;
    private final SocketAddress remoteAddress;

    public SessionEndpoint(
            final SocketAddress localAddress,
            final SocketAddress remoteAddress) {
        super();
        Args.notNull(remoteAddress, "Remote address");
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
    }

    public SessionEndpoint(final SocketAddress remoteAddress) {
        this(null, remoteAddress);
    }

    public SocketAddress getLocalAddress() {
        return localAddress;
    }

    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public int hashCode() {
        int hash = LangUtils.HASH_SEED;
        hash = LangUtils.hashCode(hash, this.localAddress);
        hash = LangUtils.hashCode(hash, this.remoteAddress);
        return hash;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SessionEndpoint) {
            final SessionEndpoint that = (SessionEndpoint) obj;
            return LangUtils.equals(this.localAddress, that.localAddress)
                && LangUtils.equals(this.remoteAddress, that.remoteAddress);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return remoteAddress.toString();
    }

}
