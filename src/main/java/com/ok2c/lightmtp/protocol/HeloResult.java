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
package com.ok2c.lightmtp.protocol;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import com.ok2c.lightmtp.SMTPReply;

public final class HeloResult {

    private final SMTPReply reply;
    private final Set<String> extensions;
    
    public HeloResult(final SMTPReply reply, final Collection<String> exts) {
        super();
        if (reply == null) {
            throw new IllegalArgumentException("SMTP reply may not be null");
        }
        this.reply = reply;
        Set<String> set = new LinkedHashSet<String>();
        if (exts != null) {
            for (String ext: exts) {
                set.add(ext.toUpperCase(Locale.US));
            }
        }
        this.extensions = Collections.unmodifiableSet(set);
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("[");
        buffer.append(this.reply.getCode());
        buffer.append("]");
        buffer.append(this.extensions);
        return super.toString();
    }
    
}
