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

import java.net.SocketAddress;

import com.ok2c.lightnio.IOSession;
import com.ok2c.lightnio.impl.pool.PoolEntry;
import com.ok2c.lightnio.impl.pool.SessionPool;
import com.ok2c.lightnio.pool.ManagedIOSession;

class MailIOSession implements ManagedIOSession {

    private final SessionPool<SocketAddress> pool;
    private final PoolEntry<SocketAddress> entry;

    private volatile boolean released;
    private volatile boolean reusable;

    public MailIOSession(
            final SessionPool<SocketAddress> pool,
            final PoolEntry<SocketAddress> entry) {
        super();
        this.pool = pool;
        this.entry = entry;
        this.released = false;
        this.reusable = true;
    }

    public void releaseSession() {
        if (this.released) {
            return;
        }
        this.released = true;
        IOSession iosession = this.entry.getIOSession();
        this.pool.release(this.entry, this.reusable && !iosession.isClosed());
    }

    public void abortSession() {
        if (this.released) {
            return;
        }
        this.released = true;
        IOSession iosession = this.entry.getIOSession();
        iosession.shutdown();
        this.pool.release(this.entry, false);
    }

    public IOSession getSession() {
        if (this.released) {
            return null;
        }
        return this.entry.getIOSession();
    }

    public Object getState() {
        if (this.released) {
            return null;
        }
        return this.entry.getState();
    }

    public void setState(final Object state) {
        if (this.released) {
            return;
        }
        this.entry.setState(state);
    }

    public boolean isReusable() {
        return this.reusable;
    }

    public void markNonReusable() {
        if (this.released) {
            return;
        }
        this.reusable = false;
    }

    public void markReusable() {
        if (this.released) {
            return;
        }
        this.reusable = true;
    }

    protected PoolEntry<SocketAddress> getEntry() {
        return this.entry;
    }

    @Override
    public String toString() {
        SocketAddress address = this.entry.getRoute();
        StringBuilder buffer = new StringBuilder();
        buffer.append("Connection to ");
        buffer.append(address);
        if (this.released) {
            buffer.append(" (released)");
        }
        return buffer.toString();
    }
    
}
