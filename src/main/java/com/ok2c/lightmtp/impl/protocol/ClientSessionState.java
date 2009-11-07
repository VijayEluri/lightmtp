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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ok2c.lightmtp.SMTPConsts;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.protocol.DeliveryRequest;
import com.ok2c.lightmtp.protocol.RcptResult;
import com.ok2c.lightnio.SessionBufferStatus;
import com.ok2c.lightnio.SessionInputBuffer;
import com.ok2c.lightnio.SessionOutputBuffer;
import com.ok2c.lightnio.impl.SessionInputBufferImpl;
import com.ok2c.lightnio.impl.SessionOutputBufferImpl;

public class ClientSessionState implements SessionBufferStatus {

    private final static int BUF_SIZE = 8 * 1024;
    private final static int LINE_SIZE = 1 * 1024;

    private final SessionInputBuffer inbuf;
    private final SessionOutputBuffer outbuf;
    private final Set<String> extensions;
    private final List<RcptResult> failures;

    private DeliveryRequest request;
    private SMTPReply reply;
    private boolean terminated;
    
    public ClientSessionState() {
        super();
        this.inbuf = createSessionInputBuffer();
        this.outbuf = createSessionOutputBuffer();
        this.failures = new ArrayList<RcptResult>();
        this.extensions = new HashSet<String>();
    }

    protected SessionInputBuffer createSessionInputBuffer() {
        SessionInputBuffer buf = new SessionInputBufferImpl(BUF_SIZE, LINE_SIZE, SMTPConsts.ASCII);
        Log wirelog = LogFactory.getLog(Wire.WIRELOG_CAT);
        if (wirelog.isDebugEnabled()) {
            buf = new LoggingSessionInputBuffer(buf, new Wire(wirelog), SMTPConsts.ASCII);
        }
        return buf;
    }

    protected SessionOutputBuffer createSessionOutputBuffer() {
        SessionOutputBuffer buf = new SessionOutputBufferImpl(BUF_SIZE, LINE_SIZE, SMTPConsts.ASCII);
        Log wirelog = LogFactory.getLog(Wire.WIRELOG_CAT);
        if (wirelog.isDebugEnabled()) {
            buf = new LoggingSessionOutputBuffer(buf, new Wire(wirelog), SMTPConsts.ASCII);
        }
        return buf;
    }

    public boolean hasBufferedInput() {
        return this.inbuf.hasData();
    }

    public boolean hasBufferedOutput() {
        return this.outbuf.hasData();
    }

    public SessionInputBuffer getInbuf() {
        return this.inbuf;
    }

    public SessionOutputBuffer getOutbuf() {
        return this.outbuf;
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
        buffer.append("[in buf: ");
        buffer.append(this.inbuf.length());
        buffer.append("][out buf: ");
        buffer.append(this.outbuf.length());
        buffer.append("][extensions: ");
        buffer.append(this.extensions);
        buffer.append("][last reply: ");
        buffer.append(this.reply);
        buffer.append("][failures: ");
        buffer.append(this.failures);
        buffer.append("]");
        return buffer.toString();
    }

}
