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

import org.apache.http.nio.reactor.SessionBufferStatus;
import org.apache.http.nio.reactor.SessionInputBuffer;
import org.apache.http.nio.reactor.SessionOutputBuffer;

import com.ok2c.lightmtp.SMTPConsts;

public class SMTPBuffers implements SessionBufferStatus {

    private final static int BUF_SIZE = 8 * 1024;
    private final static int LINE_SIZE = 1 * 1024;

    private final SMTPInputBuffer inbuf;
    private final SMTPOutputBuffer outbuf;

    public SMTPBuffers() {
        super();
        this.inbuf = new SMTPInputBuffer(BUF_SIZE, LINE_SIZE, SMTPConsts.ISO_8859_1);
        this.outbuf = new SMTPOutputBuffer(BUF_SIZE, LINE_SIZE, SMTPConsts.ISO_8859_1);
    }

    public void reset() {

    }

    @Override
    public boolean hasBufferedInput() {
        return this.inbuf.hasData();
    }

    @Override
    public boolean hasBufferedOutput() {
        return this.outbuf.hasData();
    }

    public SessionInputBuffer getInbuf() {
        return this.inbuf;
    }

    public SessionOutputBuffer getOutbuf() {
        return this.outbuf;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("[in buf: ");
        buffer.append(this.inbuf.length());
        buffer.append("][out buf: ");
        buffer.append(this.outbuf.length());
        buffer.append("]");
        return buffer.toString();
    }

}
