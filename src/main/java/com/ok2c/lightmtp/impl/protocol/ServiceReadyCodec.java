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

import java.io.IOException;
import java.nio.channels.SelectionKey;

import com.ok2c.lightmtp.SMTPCodes;
import com.ok2c.lightmtp.SMTPConsts;
import com.ok2c.lightmtp.SMTPProtocolException;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.message.SMTPMessageWriter;
import com.ok2c.lightmtp.message.SMTPReplyWriter;
import com.ok2c.lightmtp.protocol.ProtocolCodec;
import com.ok2c.lightmtp.protocol.ProtocolCodecs;
import com.ok2c.lightnio.IOSession;
import com.ok2c.lightnio.SessionOutputBuffer;

public class ServiceReadyCodec implements ProtocolCodec<ServerState> {

    private final SMTPBuffers iobuffers;
    private final SMTPMessageWriter<SMTPReply> writer;

    private SMTPReply pendingReply;
    private boolean completed;

    public ServiceReadyCodec(final SMTPBuffers iobuffers) {
        super();
        if (iobuffers == null) {
            throw new IllegalArgumentException("IO buffers may not be null");
        }
        this.iobuffers = iobuffers;
        this.writer = new SMTPReplyWriter();
    }

    public void cleanUp() {
    }

    public void reset(
            final IOSession iosession,
            final ServerState sessionState) throws IOException, SMTPProtocolException {
        this.writer.reset();
        this.pendingReply = new SMTPReply(SMTPCodes.SERVICE_READY, null,
                sessionState.getServerId() + " service ready");
        this.iobuffers.setInputCharset(SMTPConsts.ASCII);
        this.completed = false;

        iosession.setEventMask(SelectionKey.OP_WRITE);
    }

    public void produceData(
            final IOSession iosession,
            final ServerState sessionState) throws IOException, SMTPProtocolException {
        if (iosession == null) {
            throw new IllegalArgumentException("IO session may not be null");
        }
        if (sessionState == null) {
            throw new IllegalArgumentException("Session state may not be null");
        }

        SessionOutputBuffer buf = this.iobuffers.getOutbuf();

        if (this.pendingReply != null) {
            this.writer.write(this.pendingReply, buf);
            this.pendingReply = null;
        }

        if (buf.hasData()) {
            buf.flush(iosession.channel());
        }
        if (!buf.hasData()) {
            this.completed = true;
            iosession.setEventMask(SelectionKey.OP_READ);
        }
    }

    public void consumeData(
            final IOSession iosession,
            final ServerState sessionState) throws IOException, SMTPProtocolException {
    }

    public boolean isCompleted() {
        return this.completed;
    }

    public String next(
            final ProtocolCodecs<ServerState> codecs,
            final ServerState sessionState) {
        if (isCompleted()) {
            return ProtocolState.MAIL.name();
        } else {
            return null;
        }
    }

}
