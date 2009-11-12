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
package com.ok2c.lightmtp.impl.protocol;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.protocol.DeliveryRequest;
import com.ok2c.lightmtp.protocol.RcptResult;

public class ClientState {

    private final Set<String> extensions;
    private final List<RcptResult> failures;

    private DeliveryRequest request;
    private SMTPReply reply;
    private boolean terminated;
    
    public ClientState() {
        super();
        this.failures = new ArrayList<RcptResult>();
        this.extensions = new HashSet<String>();
    }

    public void reset(final DeliveryRequest request) {
        this.request = request;
        this.failures.clear();
        this.reply = null;
    }

    public DeliveryRequest getRequest() {
        return this.request;
    }

    public List<RcptResult> getFailures() {
        return this.failures;
    }

    public Set<String> getExtensions() {
        return this.extensions;
    }

    public SMTPReply getReply() {
        return this.reply;
    }

    public void setReply(final SMTPReply reply) {
        this.reply = reply;
    }

    public boolean isTerminated() {
        return this.terminated;
    }

    public void terminated() {
        this.terminated = true;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("[extensions: ");
        buffer.append(this.extensions);
        buffer.append("][last reply: ");
        buffer.append(this.reply);
        buffer.append("][failures: ");
        buffer.append(this.failures);
        buffer.append("]");
        return buffer.toString();
    }

}
