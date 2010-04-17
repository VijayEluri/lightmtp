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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ok2c.lightmtp.SMTPCodes;
import com.ok2c.lightmtp.SMTPProtocolException;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.protocol.DeliveryRequest;
import com.ok2c.lightmtp.protocol.DeliveryRequestHandler;
import com.ok2c.lightmtp.protocol.DeliveryResult;
import com.ok2c.lightmtp.protocol.BasicDeliveryResult;
import com.ok2c.lightmtp.protocol.ProtocolCodec;
import com.ok2c.lightmtp.protocol.ProtocolCodecs;
import com.ok2c.lightmtp.protocol.ServiceRefusedException;
import com.ok2c.lightmtp.protocol.SessionContext;
import com.ok2c.lightnio.IOSession;

public class ClientSession {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final IOSession iosession;
    private final SMTPBuffers iobuffers;
    private final ClientState sessionState;
    private final SessionContext context;
    private final DeliveryRequestHandler handler;
    private final ProtocolCodecs<ClientState> codecs;

    private ProtocolCodec<ClientState> currentCodec;

    private ProtocolState state;

    public ClientSession(
            final IOSession iosession,
            final SMTPBuffers iobuffers,
            final DeliveryRequestHandler handler,
            final ProtocolCodecs<ClientState> codecs) {
        super();
        if (iosession == null) {
            throw new IllegalArgumentException("IO session may not be null");
        }
        if (iobuffers == null) {
            throw new IllegalArgumentException("IO buffers may not be null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Delivery request handler may not be null");
        }
        if (codecs == null) {
            throw new IllegalArgumentException("Protocol codecs may not be null");
        }
        Logger log = LoggerFactory.getLogger(iosession.getClass());
        Logger wirelog = LoggerFactory.getLogger(Wire.WIRELOG_CAT);
        if (log.isDebugEnabled() || wirelog.isDebugEnabled()) {
            this.iosession = new LoggingIOSession(iosession, "client", log, new Wire(wirelog));
        } else {
            this.iosession = iosession;
        }
        this.iobuffers = iobuffers;
        this.iosession.setBufferStatus(this.iobuffers);
        this.sessionState = new ClientState();
        this.context = new SessionContextImpl(iosession);
        this.handler = handler;
        this.codecs = codecs;

        this.state = ProtocolState.INIT;
    }

    private void signalDeliveryReady() {
        if (this.sessionState.getRequest() != null) {
            throw new IllegalStateException("Delivery request is not null");
        }
        this.log.debug("Ready for delivery request");

        DeliveryRequest request = this.handler.submitRequest(this.context);
        this.sessionState.reset(request);

        if (request == null) {
            this.iosession.clearEvent(SelectionKey.OP_WRITE);
            this.log.debug("No delivery request submitted");
        } else {
            this.iosession.setEvent(SelectionKey.OP_WRITE);
            if (this.log.isDebugEnabled()) {
                this.log.debug("Delivery request submitted: " + request);
            }
        }
    }

    private void signalException(final Exception ex) {
        this.currentCodec.cleanUp();
        this.handler.exception(ex, this.context);

        DeliveryRequest request = this.sessionState.getRequest();
        this.sessionState.reset(null);
        if (request != null) {
            this.handler.failed(request, null, this.context);
            this.log.error("Delivery failed: " + request, ex);
        } else {
            this.log.error(ex.getMessage(), ex);
        }
    }

    private void signalDeliveryFailure() {
        DeliveryRequest request = this.sessionState.getRequest();
        if (request == null) {
            throw new IllegalStateException("Delivery request is null");
        }
        DeliveryResult result = new BasicDeliveryResult(
                this.sessionState.getReply(),
                this.sessionState.getFailures());
        this.sessionState.reset(null);
        this.handler.failed(request, result, this.context);
        if (this.log.isDebugEnabled()) {
            this.log.debug("Delivery failed: " + request + "; result: " + result);
        }
    }

    private void signalDeliverySuccess() {
        DeliveryRequest request = this.sessionState.getRequest();
        if (request == null) {
            throw new IllegalStateException("Delivery request is null");
        }
        DeliveryResult result = new BasicDeliveryResult(
                this.sessionState.getReply(),
                this.sessionState.getFailures());
        this.sessionState.reset(null);
        this.handler.completed(request, result, this.context);
        if (this.log.isDebugEnabled()) {
            this.log.debug("Delivery succeeded: " + request + "; result: " + result);
        }
    }

    public void connected() {
        if (this.state != ProtocolState.INIT) {
            throw new IllegalStateException("Unexpected state: " + this.state);
        }
        try {
            doConnected();
        } catch (IOException ex) {
            signalException(ex);
            this.iosession.close();
        } catch (SMTPProtocolException ex) {
            signalException(ex);
            this.iosession.close();
        }
    }

    public void consumeData() {
        try {
            doConsumeData();
        } catch (IOException ex) {
            signalException(ex);
            this.iosession.close();
        } catch (SMTPProtocolException ex) {
            signalException(ex);
            this.iosession.close();
        }
    }

    public void produceData() {
        try {
            doProduceData();
        } catch (IOException ex) {
            signalException(ex);
            this.iosession.close();
        } catch (SMTPProtocolException ex) {
            signalException(ex);
            this.iosession.close();
        }
    }

    public void timeout() {
        try {
            doTimeout();
        } catch (IOException ex) {
            this.currentCodec.cleanUp();
            this.iosession.close();
        } catch (SMTPProtocolException ex) {
            this.currentCodec.cleanUp();
            this.iosession.close();
        }
    }

    public void disconneced() {
        this.log.debug("Session terminated");
        this.handler.disconnected(this.context);
    }

    private void doConnected() throws IOException, SMTPProtocolException {
        if (this.log.isDebugEnabled()) {
            this.log.debug("New client connection: " + this.iosession.getRemoteAddress());
        }

        this.currentCodec = this.codecs.getCodec(ProtocolState.HELO.name());
        this.currentCodec.reset(this.iosession, this.sessionState);

        this.state = ProtocolState.HELO;

        this.handler.connected(this.context);
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
        if (this.currentCodec.isCompleted()) {

            SMTPReply reply = this.sessionState.getReply();
            if (reply != null) {
                if (this.log.isDebugEnabled()) {
                    this.log.debug(this.state + " codec completed with reply: " + reply);
                }

                switch (this.state) {
                case HELO:
                    if (reply.getCode() != SMTPCodes.OK) {
                        throw new ServiceRefusedException(reply);
                    }
                    break;
                case MAIL:
                    if (reply.getCode() != SMTPCodes.START_MAIL_INPUT) {
                        if (this.sessionState.getRequest() == null) {
                            break;
                        }
                        signalDeliveryFailure();
                    }
                    break;
                case DATA:
                    if (reply.getCode() == SMTPCodes.OK) {
                        signalDeliverySuccess();
                    } else {
                        signalDeliveryFailure();
                    }
                    break;
                }

                if (reply.getCode() == SMTPCodes.ERR_TRANS_SERVICE_NOT_AVAILABLE) {
                    this.sessionState.terminated();
                    this.iosession.close();
                }
            }
        }

        String nextCodec = this.currentCodec.next(this.codecs, this.sessionState);
        if (nextCodec != null) {
            this.state = ProtocolState.valueOf(nextCodec);
            if (this.log.isDebugEnabled()) {
                this.log.debug("Next codec: " + this.state);
            }
            this.currentCodec = this.codecs.getCodec(nextCodec);
            this.currentCodec.reset(this.iosession, this.sessionState);

            if (this.state == ProtocolState.MAIL) {
                signalDeliveryReady();
            }
        }

        ProtocolState token = (ProtocolState) this.iosession.getAttribute(ProtocolState.ATTRIB);
        if (token != null && token.equals(ProtocolState.QUIT)) {
            this.log.debug("Session termination requested");
            this.sessionState.terminated();
            this.iosession.setEvent(SelectionKey.OP_WRITE);
        }
    }

    private void doTimeout() throws IOException, SMTPProtocolException {
        this.log.debug("Session timed out");
        this.sessionState.terminated();
        this.iosession.setEvent(SelectionKey.OP_WRITE);
    }

}
