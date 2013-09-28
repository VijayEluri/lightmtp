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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.SessionInputBuffer;
import org.apache.http.nio.reactor.SessionOutputBuffer;
import org.apache.http.util.Args;

import com.ok2c.lightmtp.SMTPCode;
import com.ok2c.lightmtp.SMTPCodes;
import com.ok2c.lightmtp.SMTPCommand;
import com.ok2c.lightmtp.SMTPErrorException;
import com.ok2c.lightmtp.SMTPProtocolException;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.impl.protocol.cmd.SimpleAction;
import com.ok2c.lightmtp.message.SMTPCommandParser;
import com.ok2c.lightmtp.message.SMTPMessageParser;
import com.ok2c.lightmtp.message.SMTPMessageWriter;
import com.ok2c.lightmtp.message.SMTPReplyWriter;
import com.ok2c.lightmtp.protocol.Action;
import com.ok2c.lightmtp.protocol.ProtocolCodec;
import com.ok2c.lightmtp.protocol.ProtocolCodecs;
import com.ok2c.lightmtp.protocol.ProtocolHandler;

public class PipeliningReceiveEnvelopCodec implements ProtocolCodec<ServerState> {

    private final SMTPBuffers iobuffers;
    private final ProtocolHandler<ServerState> commandHandler;
    private final SMTPMessageParser<SMTPCommand> parser;
    private final SMTPMessageWriter<SMTPReply> writer;
    private final Queue<Action<ServerState>> pendingActions;

    private Future<SMTPReply> actionFuture;
    private boolean completed;

    public PipeliningReceiveEnvelopCodec(
            final SMTPBuffers iobuffers,
            final ProtocolHandler<ServerState> commandHandler) {
        super();
        Args.notNull(iobuffers, "IO buffers");
        Args.notNull(commandHandler, "Command handler");
        this.iobuffers = iobuffers;
        this.commandHandler = commandHandler;
        this.parser = new SMTPCommandParser();
        this.writer = new SMTPReplyWriter(true);
        this.pendingActions = new LinkedList<Action<ServerState>>();
        this.completed = false;
    }

    @Override
    public void cleanUp() {
    }

    @Override
    public void reset(
            final IOSession iosession,
            final ServerState sessionState) throws IOException, SMTPProtocolException {
        this.parser.reset();
        this.writer.reset();
        this.pendingActions.clear();
        this.actionFuture = null;
        this.completed = false;
    }

    private SMTPReply getReply(final Future<SMTPReply> future) {
        try {
            return future.get();
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause == null) {
                cause = ex;
            }
            return new SMTPReply(SMTPCodes.ERR_PERM_TRX_FAILED, new SMTPCode(5, 3, 0),
                    cause.getMessage());
        } catch (InterruptedException ex) {
            return new SMTPReply(SMTPCodes.ERR_PERM_TRX_FAILED, new SMTPCode(5, 3, 0),
                    ex.getMessage());
        }
    }

    @Override
    public void produceData(
            final IOSession iosession,
            final ServerState sessionState) throws IOException, SMTPProtocolException {
        Args.notNull(iosession, "IO session");
        Args.notNull(sessionState, "Session state");

        SessionOutputBuffer buf = this.iobuffers.getOutbuf();

        synchronized (sessionState) {

            if (this.actionFuture != null) {
                SMTPReply reply = getReply(this.actionFuture);
                this.actionFuture = null;
                this.writer.write(reply, buf);
            }

            if (this.actionFuture == null) {
                while (!this.pendingActions.isEmpty()) {
                    Action<ServerState> action = this.pendingActions.remove();
                    Future<SMTPReply> future = action.execute(
                            sessionState,
                            new OutputTrigger<SMTPReply>(sessionState, iosession));
                    if (future.isDone()) {
                        SMTPReply reply = getReply(future);
                        this.writer.write(reply, buf);
                    } else {
                        this.actionFuture = future;
                        break;
                    }
                }
            }

            if (buf.hasData()) {
                buf.flush(iosession.channel());
            }
            if (!buf.hasData()) {
                if (sessionState.getDataType() != null) {
                    this.completed = true;
                }
                if (sessionState.isTerminated()) {
                    iosession.close();
                } else {
                    iosession.clearEvent(SelectionKey.OP_WRITE);
                }
            }
        }
    }

    @Override
    public void consumeData(
            final IOSession iosession,
            final ServerState sessionState) throws IOException, SMTPProtocolException {
        Args.notNull(iosession, "IO session");
        Args.notNull(sessionState, "Session state");

        SessionInputBuffer buf = this.iobuffers.getInbuf();

        synchronized (sessionState) {
            for (;;) {
                int bytesRead = buf.fill(iosession.channel());
                try {
                    SMTPCommand command = this.parser.parse(buf, bytesRead == -1);
                    if (command == null) {
                        if (bytesRead == -1 && !sessionState.isTerminated()
                                && this.pendingActions.isEmpty()) {
                            throw new UnexpectedEndOfStreamException();
                        } else {
                            break;
                        }
                    }
                    Action<ServerState> action = this.commandHandler.handle(command);
                    this.pendingActions.add(action);
                } catch (SMTPErrorException ex) {
                    SMTPReply reply = new SMTPReply(ex.getCode(),
                            ex.getEnhancedCode(),
                            ex.getMessage());
                    this.pendingActions.add(new SimpleAction(reply));
                } catch (SMTPProtocolException ex) {
                    SMTPReply reply = new SMTPReply(SMTPCodes.ERR_PERM_SYNTAX_ERR_COMMAND,
                            new SMTPCode(5, 3, 0),
                            ex.getMessage());
                    this.pendingActions.add(new SimpleAction(reply));
                }
            }

            if (!this.pendingActions.isEmpty()) {
                iosession.setEvent(SelectionKey.OP_WRITE);
            }
        }
    }

    @Override
    public boolean isCompleted() {
        return this.completed;
    }

    @Override
    public String next(
            final ProtocolCodecs<ServerState> codecs,
            final ServerState sessionState) {
        if (isCompleted()) {
            if (sessionState.isTerminated()) {
                return ProtocolState.QUIT.name();
            }
            return ProtocolState.DATA.name();
        } else {
            return null;
        }
    }

}
