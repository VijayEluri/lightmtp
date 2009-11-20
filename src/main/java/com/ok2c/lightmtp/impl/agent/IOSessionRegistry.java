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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.ok2c.lightnio.IOSession;

public class IOSessionRegistry {

    private final Set<IOSession> sessions;
    
    public IOSessionRegistry() {
        super();
        this.sessions = new HashSet<IOSession>();
    }
    
    public synchronized void add(final IOSession iosession) {
        if (iosession == null) {
            return;
        }
        this.sessions.add(iosession);
        notifyAll();
    }

    public synchronized void remove(final IOSession iosession) {
        if (iosession == null) {
            return;
        }
        this.sessions.remove(iosession);
        notifyAll();
    }

    public synchronized boolean isEmpty() {
        return this.sessions.isEmpty();
    }
    
    public Iterator<IOSession> iterator() {
        return this.sessions.iterator();
    }
    
    public synchronized boolean awaitShutdown(
            int timeout, final TimeUnit unit) throws InterruptedException{
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        long remaining = timeout;
        boolean empty = false;
        while (!empty) {
            wait(remaining);
            empty = this.sessions.isEmpty();            
            if (timeout > 0) {
                remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    break;
                }
            }
        }
        return empty;
    }

    @Override
    public synchronized String toString() {
        return this.sessions.toString();
    }
    
}
