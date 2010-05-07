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
import java.util.List;

import org.slf4j.Logger;

import com.ok2c.lightmtp.SMTPProtocolException;
import com.ok2c.lightmtp.protocol.ProtocolCodec;
import com.ok2c.lightmtp.protocol.ProtocolCodecs;
import com.ok2c.lightnio.IOSession;

public class ServerSession {

    private final Logger log;
    private final IOSession iosession;
    private final SMTPBuffers iobuffers;
    private final ServerState sessionState;
    private final ProtocolCodecs<ServerState> codecs;

    private ProtocolCodec<ServerState> currentCodec;
    private ProtocolState state;

    public ServerSession(
            final Logger log,
            final Logger iolog,
            final Logger wirelog,
            final IOSession iosession,
            final SMTPBuffers iobuffers,
            final ProtocolCodecs<ServerState> codecs) {
        super();
        if (log == null) {
            throw new IllegalArgumentException("Logger may not be null");
        }
        if (iolog == null) {
            throw new IllegalArgumentException("IO Logger may not be null");
        }
        if (wirelog == null) {
            throw new IllegalArgumentException("Wire Logger may not be null");
        }
        if (iosession == null) {
            throw new IllegalArgumentException("IO session may not be null");
        }
        if (iobuffers == null) {
            throw new IllegalArgumentException("IO buffers may not be null");
        }
        if (codecs == null) {
            throw new IllegalArgumentException("Protocol codecs may not be null");
        }
        this.log = log;
        if (iolog.isDebugEnabled() || wirelog.isDebugEnabled()) {
            this.iosession = new LoggingIOSession(iosession, "server", iolog, new Wire(wirelog));
        } else {
            this.iosession = iosession;
        }
        this.iobuffers = iobuffers;
        this.iosession.setBufferStatus(this.iobuffers);
        this.sessionState = new ServerState("LightMTP SMTP");
        this.codecs = codecs;
        this.state = ProtocolState.INIT;
    }

    private void terminate() {
        this.sessionState.reset();
        if (this.currentCodec != null) {
            this.currentCodec.cleanUp();
        }
        this.iosession.close();
    }

    private void handleIOException(final IOException ex) {
        String messageId = this.sessionState.getMessageId();
        terminate();
        if (this.log.isInfoEnabled()) {
            if (messageId != null) {
                this.log.info("Failure receiving message %s", messageId);
            }
        }
        this.log.error("Fatal I/O error: " + ex.getMessage(), ex);
    }

    private void handleSMTPException(final SMTPProtocolException ex) {
        String messageId = this.sessionState.getMessageId();
        terminate();
        if (this.log.isInfoEnabled()) {
            if (messageId != null) {
                this.log.info("Failure receiving message %s", messageId);
            }
        }
        this.log.error("Fatal protocol error: " + ex.getMessage(), ex);
    }

    public void connected() {
        if (this.state != ProtocolState.INIT) {
            throw new IllegalStateException("Unexpected state: " + this.state);
        }
        try {
            doConnected();
        } catch (IOException ex) {
            handleIOException(ex);
        } catch (SMTPProtocolException ex) {
            handleSMTPException(ex);
        }
    }

    public void consumeData() {
        try {
            doConsumeData();
        } catch (IOException ex) {
            handleIOException(ex);
        } catch (SMTPProtocolException ex) {
            handleSMTPException(ex);
        }
    }

    public void produceData() {
        try {
            doProduceData();
        } catch (IOException ex) {
            handleIOException(ex);
        } catch (SMTPProtocolException ex) {
            handleSMTPException(ex);
        }
    }

    public void timeout() {
        if (this.log.isDebugEnabled()) {
            this.log.error("Connection timed out: " + this.iosession.getRemoteAddress());
        }
        this.sessionState.terminated();
        this.iosession.setEvent(SelectionKey.OP_WRITE);
    }

    public void disconneced() {
        this.log.debug("Session terminated");
    }

    private void doConnected() throws IOException, SMTPProtocolException {
        if (this.log.isDebugEnabled()) {
            this.log.debug("New incoming connection: " + this.iosession.getRemoteAddress());
        }
        this.currentCodec = this.codecs.getCodec(ProtocolState.INIT.name());
        this.currentCodec.reset(this.iosession, this.sessionState);
    }

    private void doConsumeData() throws IOException, SMTPProtocolException {
        this.log.debug("Consume data");
        this.currentCodec.consumeData(this.iosession, this.sessionState);
        updateSession();
    }

    private void doProduceData() throws IOException, SMTPProtocolException {
        this.log.debug("Produce data");
        this.currentCodec.produceData(this.iosession, this.sessionState);
        updateSession();
    }

    private void updateSession() throws IOException, SMTPProtocolException {
        String nextCodec = this.currentCodec.next(this.codecs, this.sessionState);
        if (nextCodec != null) {
            this.state = ProtocolState.valueOf(nextCodec);
            if (this.log.isDebugEnabled()) {
                this.log.debug("Next codec: " + this.state);
            }
            this.currentCodec = this.codecs.getCodec(nextCodec);
            this.currentCodec.reset(this.iosession, this.sessionState);

            if (this.log.isDebugEnabled()) {
                switch (this.state) {
                case DATA:
                    String messageId = this.sessionState.getMessageId();
                    String sender = this.sessionState.getSender();
                    List<String> recipients = this.sessionState.getRecipients();
                    this.log.debug("Incoming message "
                            + messageId + " [" + sender + "] -> " + recipients);
                    break;
                }
            }
        }
        ProtocolState token = (ProtocolState) this.iosession.getAttribute(ProtocolState.ATTRIB);
        if (token != null && token.equals(ProtocolState.QUIT)) {
            this.log.debug("Session termination requested");
            this.sessionState.terminated();
            this.iosession.setEvent(SelectionKey.OP_WRITE);
        }
    }

}
