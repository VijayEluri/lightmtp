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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ok2c.lightmtp.SMTPReply;

public class BasicDeliveryResult implements DeliveryResult {

    private final SMTPReply reply;
    private final List<RcptResult> failures;

    public BasicDeliveryResult(final SMTPReply reply, List<RcptResult> rcptFailures) {
        super();
        if (reply == null) {
            throw new IllegalArgumentException("SMTP reply may not be null");
        }
        this.reply = reply;
        ArrayList<RcptResult> list = new ArrayList<RcptResult>();
        if (rcptFailures != null) {
            list.addAll(rcptFailures);
        }
        this.failures = Collections.unmodifiableList(list);
    }

    public BasicDeliveryResult(final SMTPReply reply) {
        this(reply, null);
    }

    public SMTPReply getReply() {
        return this.reply;
    }

    public List<RcptResult> getFailures() {
        return this.failures;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("[");
        buffer.append(this.reply);
        buffer.append("][failures: ");
        buffer.append(this.failures);
        buffer.append("]");
        return buffer.toString();
    }

}
