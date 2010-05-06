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

import java.util.Formatter;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import com.ok2c.lightmtp.protocol.UniqueIdGenerator;

public class SimpleIdGenerator implements UniqueIdGenerator {

    private final static AtomicLong COUNT = new AtomicLong(0L);

    public SimpleIdGenerator() {
        super();
    }

    public String generate() {
        StringBuilder buffer = new StringBuilder();
        Formatter formatter = new Formatter(buffer, Locale.US);
        formatter.format("%1$016x", COUNT.incrementAndGet());
        return buffer.toString();
    }

}
