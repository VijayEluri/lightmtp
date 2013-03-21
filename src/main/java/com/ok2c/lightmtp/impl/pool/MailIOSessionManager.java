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
package com.ok2c.lightmtp.impl.pool;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.http.concurrent.BasicFuture;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.pool.PoolStats;
import org.apache.http.util.Args;
import org.apache.http.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ok2c.lightmtp.agent.SessionEndpoint;

public class MailIOSessionManager {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final CPool pool;

    public MailIOSessionManager(final ConnectingIOReactor ioreactor) {
        super();
        Args.notNull(ioreactor, "I/O reactor");
        this.pool = new CPool(ioreactor, 20, 50);
    }

    private String format(final SessionEndpoint endpoint, final Object state) {
        final StringBuilder buf = new StringBuilder();
        buf.append("[address: ").append(endpoint).append("]");
        if (state != null) {
            buf.append("[state: ").append(state).append("]");
        }
        return buf.toString();
    }

    private String formatStats(final SessionEndpoint endpoint) {
        final StringBuilder buf = new StringBuilder();
        final PoolStats totals = this.pool.getTotalStats();
        final PoolStats stats = this.pool.getStats(endpoint);
        buf.append("[total kept alive: ").append(totals.getAvailable()).append("; ");
        buf.append("address allocated: ").append(stats.getLeased() + stats.getAvailable());
        buf.append(" of ").append(stats.getMax()).append("; ");
        buf.append("total allocated: ").append(totals.getLeased() + totals.getAvailable());
        buf.append(" of ").append(totals.getMax()).append("]");
        return buf.toString();
    }

    private String format(final LeasedSession entry) {
        final StringBuilder buf = new StringBuilder();
        buf.append("[id: ").append(entry.getId()).append("]");
        buf.append("[address: ").append(entry.getRoute()).append("]");
        final Object state = entry.getState();
        if (state != null) {
            buf.append("[state: ").append(state).append("]");
        }
        return buf.toString();
    }

    public Future<LeasedSession> leaseSession(
            final SessionEndpoint endpoint,
            final long connectTimeout,
            final TimeUnit tunit,
            final FutureCallback<LeasedSession> callback) {
        Args.notNull(endpoint, "Session endpoint");
        if (this.log.isDebugEnabled()) {
            this.log.debug("Session request: " + format(endpoint, null) + formatStats(endpoint));
        }
        final BasicFuture<LeasedSession> future = new BasicFuture<LeasedSession>(callback);
        this.pool.lease(endpoint, null, connectTimeout,
                tunit != null ? tunit : TimeUnit.MILLISECONDS,
                new InternalPoolEntryCallback(future));
        return future;
    }

    public void releaseSession(final LeasedSession managedSession) {
        Args.notNull(managedSession, "Managed session");
        synchronized (managedSession) {
            final IOSession iosession = managedSession.getConnection();
            try {
                if (!iosession.isClosed()) {
                    managedSession.updateExpiry(30, TimeUnit.SECONDS);
                    if (this.log.isDebugEnabled()) {
                        this.log.debug("Connection " + format(managedSession) +
                                " can be kept alive for 30 seconds");
                    }
                }
            } finally {
                this.pool.release(managedSession, !iosession.isClosed());
                if (this.log.isDebugEnabled()) {
                    this.log.debug("Session released: " + format(managedSession) +
                            formatStats(managedSession.getRoute()));
                }
            }
        }
    }

    public PoolStats getTotalStats() {
        return this.pool.getTotalStats();
    }

    public PoolStats getStats(final SessionEndpoint endpoint) {
        return this.pool.getStats(endpoint);
    }

    public void setMaxTotal(final int max) {
        this.pool.setMaxTotal(max);
    }

    public void setDefaultMaxPerAddress(final int max) {
        this.pool.setDefaultMaxPerRoute(max);
    }

    public void setMaxPerAddress(final SessionEndpoint endpoint, final int max) {
        this.pool.setMaxPerRoute(endpoint, max);
    }

    public void closeExpired() {
        this.pool.closeExpired();
    }

    public void closeIdle(final long idletime, final TimeUnit tunit) {
        this.pool.closeIdle(idletime, tunit);
    }

    public void shutdown() throws IOException {
        this.log.debug("I/O session manager shut down");
        this.pool.shutdown(2000);
    }

    class InternalPoolEntryCallback implements FutureCallback<LeasedSession> {

        private final BasicFuture<LeasedSession> future;

        public InternalPoolEntryCallback(final BasicFuture<LeasedSession> future) {
            super();
            this.future = future;
        }

        @Override
        public void completed(final LeasedSession entry) {
            Asserts.check(entry.getConnection() != null, "Pool entry with no connection");
            if (log.isDebugEnabled()) {
                log.debug("Connection leased: " + format(entry) + formatStats(entry.getRoute()));
            }
            if (!this.future.completed(entry)) {
                pool.release(entry, true);
            }
        }

        @Override
        public void failed(final Exception ex) {
            if (log.isDebugEnabled()) {
                log.debug("Connection request failed", ex);
            }
            this.future.failed(ex);
        }

        @Override
        public void cancelled() {
            log.debug("Connection request cancelled");
            this.future.cancel(true);
        }

    }

}
