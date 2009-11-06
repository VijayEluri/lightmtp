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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ok2c.lightmtp.SMTPProtocolException;
import com.ok2c.lightmtp.protocol.ProtocolCodec;
import com.ok2c.lightmtp.protocol.ProtocolCodecs;
import com.ok2c.lightnio.IOSession;

public class ServerSession {

    private final IOSession iosession;
    private final ServerSessionState sessionState;
    private final ProtocolCodecs<ServerSessionState> codecs;

    private final Log log;

    private ProtocolCodec<ServerSessionState> currentCodec;
    private ProtocolState state;

    public ServerSession(
            final IOSession iosession,
            final ProtocolCodecs<ServerSessionState> codecs) {
        super();
        if (iosession == null) {
            throw new IllegalArgumentException("IO session may not be null");
        }
        if (codecs == null) {
            throw new IllegalArgumentException("Protocol codecs may not be null");
        }
        Log log = LogFactory.getLog(iosession.getClass());
        if (log.isDebugEnabled()) {
            this.iosession = new LoggingIOSession(iosession, "server", log);
        } else {
            this.iosession = iosession;
        }
        this.sessionState = new ServerSessionState("LightMTP SMTP");
        this.iosession.setBufferStatus(this.sessionState);
        this.codecs = codecs;
        this.state = ProtocolState.INIT;

        this.log = LogFactory.getLog(getClass());
    }

    private void terminate() {
        this.sessionState.reset();
        if (this.currentCodec != null) {
            this.currentCodec.cleanUp();
        }
        this.iosession.close();
    }

    private void handleIOException(final IOException ex) {
        terminate();
        this.log.error("Fatal I/O error: " + ex.getMessage(), ex);
    }

    private void handleSMTPException(final SMTPProtocolException ex) {
        terminate();
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

        terminate();
    }

    public void disconneced() {
        this.log.debug("Connection terminated");
    }

    private void doConnected() throws IOException, SMTPProtocolException {
        if (this.log.isDebugEnabled()) {
            this.log.debug("New incoming connection: " + this.iosession.getRemoteAddress());
        }
        this.currentCodec = this.codecs.getCodec(ProtocolState.INIT.name());
        this.currentCodec.reset(this.iosession, this.sessionState);
    }

    private void doConsumeData() throws IOException, SMTPProtocolException {
        this.currentCodec.consumeData(this.iosession, this.sessionState);
        updateSession();
    }

    private void doProduceData() throws IOException, SMTPProtocolException {
        this.currentCodec.produceData(this.iosession, this.sessionState);
        updateSession();
    }

    private void updateSession() throws IOException, SMTPProtocolException {
        String nextCodec = this.currentCodec.next(this.codecs, this.sessionState);
        if (nextCodec != null) {
            this.state = ProtocolState.valueOf(nextCodec);
            this.currentCodec = this.codecs.getCodec(nextCodec);
            this.currentCodec.reset(this.iosession, this.sessionState);

            if (this.log.isDebugEnabled()) {
                this.log.debug("Next codec: " + this.state);
            }
        }
    }

}
