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

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ok2c.lightmtp.SMTPProtocolException;
import com.ok2c.lightmtp.impl.protocol.cmd.DataHandler;
import com.ok2c.lightmtp.impl.protocol.cmd.DefaultProtocolHandler;
import com.ok2c.lightmtp.impl.protocol.cmd.EhloHandler;
import com.ok2c.lightmtp.impl.protocol.cmd.HeloHandler;
import com.ok2c.lightmtp.impl.protocol.cmd.MailFromHandler;
import com.ok2c.lightmtp.impl.protocol.cmd.NoopHandler;
import com.ok2c.lightmtp.impl.protocol.cmd.QuitHandler;
import com.ok2c.lightmtp.impl.protocol.cmd.RcptToHandler;
import com.ok2c.lightmtp.impl.protocol.cmd.RsetHandler;
import com.ok2c.lightmtp.protocol.DeliveryHandler;
import com.ok2c.lightmtp.protocol.EnvelopValidator;
import com.ok2c.lightmtp.protocol.ProtocolCodec;
import com.ok2c.lightmtp.protocol.ProtocolCodecs;
import com.ok2c.lightmtp.protocol.ProtocolHandler;
import com.ok2c.lightnio.IOSession;

public class ServerSession {

    private final IOSession iosession;
    private final ServerSessionState sessionState;

    private final Log log;

    private ProtocolCodecs<ServerSessionState> codecs;
    private ProtocolCodec<ServerSessionState> currentCodec;

    private ProtocolState state;

    public ServerSession(
            final IOSession iosession,
            final String serverId,
            final File workingDir,
            final EnvelopValidator validator,
            final DeliveryHandler handler) {
        super();
        if (iosession == null) {
            throw new IllegalArgumentException("IO session may not be null");
        }
        if (serverId == null) {
            throw new IllegalArgumentException("Server id may not be null");
        }
        Log log = LogFactory.getLog(iosession.getClass());
        if (log.isDebugEnabled()) {
            this.iosession = new LoggingIOSession(iosession, "client", log);
        } else {
            this.iosession = iosession;
        }
        this.sessionState = new ServerSessionState(serverId);
        this.iosession.setBufferStatus(this.sessionState);
        this.codecs = new ProtocolCodecRegistry<ServerSessionState>();
        this.state = ProtocolState.INIT;

        this.log = LogFactory.getLog(getClass());

        this.codecs.register(ProtocolState.INIT.name(),
                new ServiceReadyCodec());
        this.codecs.register(ProtocolState.MAIL.name(),
                new PipeliningReceiveEnvelopCodec(createProtocolHandler(validator)));
        this.codecs.register(ProtocolState.DATA.name(),
                new ReceiveDataCodec(workingDir, handler));

        iosession.setSocketTimeout(5000);
    }

    protected ProtocolHandler<ServerSessionState> createProtocolHandler(
            final EnvelopValidator validator) {
        DefaultProtocolHandler handler = new DefaultProtocolHandler();
        handler.register("HELO", new HeloHandler(validator));
        handler.register("EHLO", new EhloHandler(validator));
        handler.register("RSET", new RsetHandler());
        handler.register("NOOP", new NoopHandler());
        handler.register("QUIT", new QuitHandler());
        handler.register("MAIL", new MailFromHandler(validator));
        handler.register("RCPT", new RcptToHandler(validator));
        handler.register("DATA", new DataHandler());
        return handler;
    }

    private void terminate() {
        this.sessionState.reset();
        if (this.currentCodec != null) {
            this.currentCodec.cleanUp();
        }
        if (!this.iosession.isClosed()) {
            this.iosession.close();
        }
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
