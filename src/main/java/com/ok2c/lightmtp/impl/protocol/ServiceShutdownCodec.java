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

import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.SessionOutputBuffer;
import org.apache.http.util.Args;

import com.ok2c.lightmtp.SMTPCode;
import com.ok2c.lightmtp.SMTPCodes;
import com.ok2c.lightmtp.SMTPProtocolException;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.message.SMTPMessageWriter;
import com.ok2c.lightmtp.message.SMTPReplyWriter;
import com.ok2c.lightmtp.protocol.ProtocolCodec;
import com.ok2c.lightmtp.protocol.ProtocolCodecs;

public class ServiceShutdownCodec implements ProtocolCodec<ServerState> {

    private final SMTPBuffers iobuffers;
    private final SMTPMessageWriter<SMTPReply> writer;

    private SMTPReply pendingReply;
    private boolean completed;

    public ServiceShutdownCodec(final SMTPBuffers iobuffers) {
        super();
        Args.notNull(iobuffers, "IO buffers");
        this.iobuffers = iobuffers;
        this.writer = new SMTPReplyWriter(true);
    }

    @Override
    public void cleanUp() {
    }

    @Override
    public void reset(
            final IOSession iosession,
            final ServerState sessionState) throws IOException, SMTPProtocolException {
        this.writer.reset();
        this.pendingReply = new SMTPReply(SMTPCodes.ERR_TRANS_SERVICE_NOT_AVAILABLE,
                new SMTPCode(4, 3, 0),
                sessionState.getServerId() + " service shutting down " +
                        "and closing transmission channel");
        this.completed = false;

        iosession.setEventMask(SelectionKey.OP_WRITE);
    }

    @Override
    public void produceData(
            final IOSession iosession,
            final ServerState sessionState) throws IOException, SMTPProtocolException {
        Args.notNull(iosession, "IO session");
        Args.notNull(sessionState, "Session state");

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
            iosession.close();
        }
    }

    @Override
    public void consumeData(
            final IOSession iosession,
            final ServerState sessionState) throws IOException, SMTPProtocolException {
    }

    @Override
    public boolean isCompleted() {
        return this.completed;
    }

    @Override
    public String next(
            final ProtocolCodecs<ServerState> codecs,
            final ServerState sessionState) {
        return null;
    }

}
