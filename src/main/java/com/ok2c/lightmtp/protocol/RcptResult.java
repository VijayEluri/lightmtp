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

import com.ok2c.lightmtp.SMTPReply;

public final class RcptResult {

    private final SMTPReply reply;
    private final String recipient;

    public RcptResult(final SMTPReply reply, final String recipient) {
        super();
        if (reply == null) {
            throw new IllegalArgumentException("SMTP reply may not be null");
        }
        if (recipient == null) {
            throw new IllegalArgumentException("Recipient may not be null");
        }
        this.reply = reply;
        this.recipient = recipient;
    }

    public SMTPReply getReply() {
        return this.reply;
    }

    public String getRecipient() {
        return this.recipient;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("[");
        buffer.append(this.recipient);
        buffer.append(": ");
        buffer.append(this.reply.getCode());
        buffer.append(" ");
        buffer.append(this.reply.getLine());
        buffer.append("]");
        return buffer.toString();
    }

}