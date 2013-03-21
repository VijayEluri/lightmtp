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

import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.util.Args;

import com.ok2c.lightmtp.message.SMTPContent;

public class BasicDeliveryRequest implements DeliveryRequest {

    private final String sender;
    private final List<String> recipients;
    private final SMTPContent<ReadableByteChannel> content;

    public BasicDeliveryRequest(
            final String sender,
            final List<String> recipients,
            final SMTPContent<ReadableByteChannel> content) {
        super();
        Args.notNull(sender, "Sender");
        if (recipients == null || recipients.isEmpty()) {
            throw new IllegalArgumentException("List of recipients may not be null or empty");
        }
        Args.notNull(content, "Delivery content");
        this.sender = sender;
        this.recipients = Collections.unmodifiableList(new ArrayList<String>(recipients));
        this.content = content;
    }

    @Override
    public String getSender() {
        return this.sender;
    }

    @Override
    public List<String> getRecipients() {
        return this.recipients;
    }

    @Override
    public SMTPContent<ReadableByteChannel> getContent() {
        return this.content;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("[");
        buffer.append(this.sender);
        buffer.append(" -> ");
        buffer.append(this.recipients);
        buffer.append("]");
        return buffer.toString();
    }

}
