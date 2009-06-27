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
package com.ok2c.lightmtp.protocol.impl;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.protocol.DeliveryRequest;
import com.ok2c.lightmtp.protocol.RcptResult;
import com.ok2c.lightnio.SessionBufferStatus;
import com.ok2c.lightnio.SessionInputBuffer;
import com.ok2c.lightnio.SessionOutputBuffer;
import com.ok2c.lightnio.impl.SessionInputBufferImpl;
import com.ok2c.lightnio.impl.SessionOutputBufferImpl;

public class SessionState implements SessionBufferStatus {

    private final static int BUF_SIZE = 8 * 1024;
    private final static int LINE_SIZE = 1 * 1024;
    private final static Charset ASCII = Charset.forName("ASCII");
    
    private final SessionInputBuffer inbuf;
    private final SessionOutputBuffer outbuf;
    private final Set<String> extensions;
    private final List<RcptResult> rcptFailures;
    
    private DeliveryRequest request;
    private SMTPReply reply;
    
    public SessionState() {
        super();
        this.inbuf = new SessionInputBufferImpl(BUF_SIZE, LINE_SIZE, ASCII);
        this.outbuf = new SessionOutputBufferImpl(BUF_SIZE, LINE_SIZE, ASCII);
        this.rcptFailures = new ArrayList<RcptResult>();
        this.extensions = new HashSet<String>();
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
        this.rcptFailures.clear();
        this.reply = null;
    }
    
    public DeliveryRequest getRequest() {
        if (this.request == null) {
            throw new IllegalStateException("SMTP request is null");
        }
        return this.request;
    }

    public List<RcptResult> getRcptFailures() {
        return this.rcptFailures;
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
    
}
