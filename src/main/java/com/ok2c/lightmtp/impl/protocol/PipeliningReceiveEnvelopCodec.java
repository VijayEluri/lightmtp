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
import java.util.LinkedList;
import java.util.Queue;

import com.ok2c.lightmtp.SMTPCode;
import com.ok2c.lightmtp.SMTPCodes;
import com.ok2c.lightmtp.SMTPCommand;
import com.ok2c.lightmtp.SMTPProtocolException;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.message.SMTPCommandParser;
import com.ok2c.lightmtp.message.SMTPMessageParser;
import com.ok2c.lightmtp.message.SMTPMessageWriter;
import com.ok2c.lightmtp.message.SMTPReplyWriter;
import com.ok2c.lightmtp.protocol.ProtocolHandler;
import com.ok2c.lightmtp.protocol.ProtocolCodec;
import com.ok2c.lightmtp.protocol.ProtocolCodecs;
import com.ok2c.lightnio.IOSession;
import com.ok2c.lightnio.SessionInputBuffer;
import com.ok2c.lightnio.SessionOutputBuffer;

public class PipeliningReceiveEnvelopCodec implements ProtocolCodec<ServerSessionState> {

    private final ProtocolHandler<ServerSessionState> commandHandler;
    private final SMTPMessageParser<SMTPCommand> parser;
    private final SMTPMessageWriter<SMTPReply> writer;
    private final Queue<SMTPReply> pendingReplies;

    private boolean idle;
    private boolean completed;

    public PipeliningReceiveEnvelopCodec(final ProtocolHandler<ServerSessionState> commandHandler) {
        super();
        if (commandHandler == null) {
            throw new IllegalArgumentException("Command handler may not be null");
        }
        this.commandHandler = commandHandler;
        this.parser = new SMTPCommandParser();
        this.writer = new SMTPReplyWriter(true);
        this.pendingReplies = new LinkedList<SMTPReply>();
        this.idle = true;
        this.completed = false;
    }

    public void cleanUp() {
    }

    public void reset(
            final IOSession iosession,
            final ServerSessionState sessionState) throws IOException, SMTPProtocolException {
        this.parser.reset();
        this.writer.reset();
        this.pendingReplies.clear();
        this.idle = true;
        this.completed = false;
    }

    public void produceData(
            final IOSession iosession,
            final ServerSessionState sessionState) throws IOException, SMTPProtocolException {
        if (iosession == null) {
            throw new IllegalArgumentException("IO session may not be null");
        }
        if (sessionState == null) {
            throw new IllegalArgumentException("Session state may not be null");
        }

        SessionOutputBuffer buf = sessionState.getOutbuf();

        while (!this.pendingReplies.isEmpty()) {
            SMTPReply reply = this.pendingReplies.remove();
            this.writer.write(reply, buf);
        }

        if (buf.hasData()) {
            buf.flush(iosession.channel());
        }
        if (!buf.hasData()) {
            if (sessionState.getDataType() != null) {
                this.completed = true;
            }
            iosession.clearEvent(SelectionKey.OP_WRITE);
        }
    }

    public void consumeData(
            final IOSession iosession,
            final ServerSessionState sessionState) throws IOException, SMTPProtocolException {
        if (iosession == null) {
            throw new IllegalArgumentException("IO session may not be null");
        }
        if (sessionState == null) {
            throw new IllegalArgumentException("Session state may not be null");
        }

        SessionInputBuffer buf = sessionState.getInbuf();

        for (;;) {
            int bytesRead = buf.fill(iosession.channel());
            try {
                SMTPCommand command = this.parser.parse(buf, bytesRead == -1);
                if (command == null) {
                    if (bytesRead == -1) {
                        throw new UnexpectedEndOfStreamException();
                    }
                    break;
                }
                SMTPReply reply = this.commandHandler.handle(command, sessionState);
                this.pendingReplies.add(reply);
            } catch (SMTPProtocolException ex) {
                SMTPCode enhancedCode = null;
                if (sessionState.isEnhancedCodeCapable()) {
                    enhancedCode = new SMTPCode(5, 5, 1);
                }
                SMTPReply reply = new SMTPReply(SMTPCodes.ERR_PERM_SYNTAX_ERR_COMMAND, 
                        enhancedCode, ex.getMessage());
                this.pendingReplies.add(reply);
            }
        }
        
        if (!this.pendingReplies.isEmpty()) {
            iosession.setEvent(SelectionKey.OP_WRITE);
        }

        this.idle = (sessionState.getSender() == null);
    }

    public boolean isIdle() {
        return this.idle;
    }

    public boolean isCompleted() {
        return this.completed;
    }

    public String next(
            final ProtocolCodecs<ServerSessionState> codecs,
            final ServerSessionState sessionState) {
        if (isCompleted()) {
            return ProtocolState.DATA.name();
        } else {
            return null;
        }
    }

}
