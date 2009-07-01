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
        if (sender == null) {
            throw new IllegalArgumentException("Sender may not be null");
        }
        if (recipients == null || recipients.isEmpty()) {
            throw new IllegalArgumentException("List of recipients may not be null or empty");
        }
        if (content == null) {
            throw new IllegalArgumentException("Delivery content may not be null");
        }
        this.sender = sender;
        this.recipients = Collections.unmodifiableList(new ArrayList<String>(recipients));
        this.content = content;
    }

    public String getSender() {
        return this.sender;
    }
    
    public List<String> getRecipients() {
        return this.recipients;
    }

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
