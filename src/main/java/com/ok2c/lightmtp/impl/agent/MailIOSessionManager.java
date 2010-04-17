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
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ok2c.lightnio.ConnectingIOReactor;
import com.ok2c.lightnio.IOSession;
import com.ok2c.lightnio.concurrent.BasicFuture;
import com.ok2c.lightnio.concurrent.FutureCallback;
import com.ok2c.lightnio.impl.pool.PoolEntry;
import com.ok2c.lightnio.impl.pool.PoolEntryCallback;
import com.ok2c.lightnio.impl.pool.RouteResolver;
import com.ok2c.lightnio.impl.pool.SessionPool;
import com.ok2c.lightnio.pool.IOSessionManager;
import com.ok2c.lightnio.pool.ManagedIOSession;
import com.ok2c.lightnio.pool.PoolStats;

class MailIOSessionManager implements IOSessionManager<SocketAddress> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final SessionPool<SocketAddress> pool;

    public MailIOSessionManager(final ConnectingIOReactor ioreactor) {
        super();
        if (ioreactor == null) {
            throw new IllegalArgumentException("I/O reactor may not be null");
        }
        this.pool = new SessionPool<SocketAddress>(
                ioreactor, new InternalRouteResolver(), 20, 50);
    }

    public synchronized Future<ManagedIOSession> leaseSession(
            final SocketAddress route,
            final Object state,
            final FutureCallback<ManagedIOSession> callback) {
        if (this.log.isDebugEnabled()) {
            this.log.debug("I/O session request: route[" + route + "][state: " + state + "]");
            PoolStats totals = this.pool.getTotalStats();
            PoolStats stats = this.pool.getStats(route);
            this.log.debug("Total: " + totals);
            this.log.debug("Route [" + route + "]: " + stats);
        }

        BasicFuture<ManagedIOSession> future = new BasicFuture<ManagedIOSession>(
                callback);
        this.pool.lease(route, state, new InternalPoolEntryCallback(future));
        if (this.log.isDebugEnabled()) {
            if (!future.isDone()) {
                this.log.debug("I/O session could not be allocated immediately: " +
                        "route[" + route + "][state: " + state + "]");
            }
        }
        return future;
    }

    public synchronized void releaseSession(final ManagedIOSession session) {
        if (!(session instanceof MailIOSession)) {
            throw new IllegalArgumentException
                ("I/O session class mismatch, " +
                 "I/O session not obtained from this manager");
        }
        session.releaseSession();
        if (this.log.isDebugEnabled()) {
            MailIOSession adaptor = (MailIOSession) session;
            PoolEntry<SocketAddress> entry = adaptor.getEntry();
            SocketAddress route = entry.getRoute();
            PoolStats totals = this.pool.getTotalStats();
            PoolStats stats = this.pool.getStats(route);
            this.log.debug("Total: " + totals);
            this.log.debug("Route [" + route + "]: " + stats);
            this.log.debug("I/O session released: " + entry);
        }
    }

    public synchronized void removeExpired(final IOSession iosession) {
        @SuppressWarnings("unchecked")
        PoolEntry<SocketAddress> entry = (PoolEntry<SocketAddress>) iosession.getAttribute(
                PoolEntry.ATTRIB);
        if (entry != null) {
            this.pool.remove(entry);
            if (this.log.isDebugEnabled()) {
                SocketAddress route = entry.getRoute();
                PoolStats totals = this.pool.getTotalStats();
                PoolStats stats = this.pool.getStats(route);
                this.log.debug("Total: " + totals);
                this.log.debug("Route [" + route + "]: " + stats);
                this.log.debug("I/O session removed: " + entry);
            }
        }
    }

    public PoolStats getTotalStats() {
        return this.pool.getTotalStats();
    }

    public PoolStats getStats(final SocketAddress route) {
        return this.pool.getStats(route);
    }

    public void setTotalMax(int max) {
        this.pool.setTotalMax(max);
    }

    public void setDefaultMaxPerHost(int max) {
        this.pool.setDefaultMaxPerHost(max);
    }

    public void setMaxPerHost(final SocketAddress route, int max) {
        this.pool.setMaxPerHost(route, max);
    }

    public synchronized void shutdown() {
        this.log.debug("I/O session manager shut down");
        this.pool.shutdown();
    }

    static class InternalRouteResolver implements RouteResolver<SocketAddress> {

        public SocketAddress resolveLocalAddress(final SocketAddress route) {
            return null;
        }

        public SocketAddress resolveRemoteAddress(final SocketAddress route) {
            return route;
        }

    }

    class InternalPoolEntryCallback implements PoolEntryCallback<SocketAddress> {

        private final BasicFuture<ManagedIOSession> future;

        public InternalPoolEntryCallback(
                final BasicFuture<ManagedIOSession> future) {
            super();
            this.future = future;
        }

        public void completed(final PoolEntry<SocketAddress> entry) {
            if (log.isDebugEnabled()) {
                log.debug("I/O session allocated: " + entry);
            }
            MailIOSession result = new MailIOSession(
                    MailIOSessionManager.this.pool,
                    entry);
            if (!this.future.completed(result)) {
                pool.release(entry, true);
            }
        }

        public void failed(final Exception ex) {
            if (log.isDebugEnabled()) {
                log.debug("I/O session request failed", ex);
            }
            this.future.failed(ex);
        }

        public void cancelled() {
            log.debug("I/O session request cancelled");
            this.future.cancel(true);
        }

    }

}
