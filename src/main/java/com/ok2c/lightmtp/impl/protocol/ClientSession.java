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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ok2c.lightmtp.SMTPCodes;
import com.ok2c.lightmtp.SMTPProtocolException;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.protocol.DeliveryRequest;
import com.ok2c.lightmtp.protocol.DeliveryRequestHandler;
import com.ok2c.lightmtp.protocol.DeliveryResult;
import com.ok2c.lightmtp.protocol.ProtocolCodec;
import com.ok2c.lightmtp.protocol.ProtocolCodecs;
import com.ok2c.lightmtp.protocol.SessionContext;
import com.ok2c.lightnio.IOSession;

public class ClientSession {
    
    private final IOSession iosession;
    private final SessionState sessionState;
    private final SessionContext context;
    private final DeliveryRequestHandler handler;
    
    private final Log log;
    
    private ProtocolCodecs<SessionState> codecs;
    private ProtocolCodec<SessionState> currentCodec;
    
    private ProtocolState state;
    
    public ClientSession(final IOSession iosession, final DeliveryRequestHandler handler) {
        super();
        if (iosession == null) {
            throw new IllegalArgumentException("IO session may not be null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Delivery request handler may not be null");
        }
        Log log = LogFactory.getLog(iosession.getClass());
        if (log.isDebugEnabled()) {
            this.iosession = new LoggingIOSession(iosession, "client", log);
        } else {
            this.iosession = iosession;
        }
        this.sessionState = new SessionState();
        this.context = new SessionContextImpl(iosession);
        this.handler = handler;
        this.iosession.setBufferStatus(this.sessionState);
        this.codecs = new ProtocolCodecRegistry<SessionState>();
        this.state = ProtocolState.INIT;
    
        this.log = LogFactory.getLog(getClass());
        
        this.codecs.register(ProtocolState.HELO.name(), new ExtendedHeloCodec());
        this.codecs.register(ProtocolState.MAIL.name(), new SimpleMailTrxCodec(false));
        this.codecs.register(ProtocolState.DATA.name(), new SendDataCodec(false));
        this.codecs.register(ProtocolState.QUIT.name(), new QuitCodec());
    }

    public void connected() throws IOException, SMTPProtocolException {
        if (this.state != ProtocolState.INIT) {
            throw new IllegalStateException("Unexpected state: " + this.state);
        }
        
        if (this.log.isDebugEnabled()) {
            this.log.debug("New client connection: " + this.iosession.getRemoteAddress());
        }
        
        this.currentCodec = this.codecs.getCodec(ProtocolState.HELO.name());
        this.currentCodec.reset(this.iosession, this.sessionState);
        
        this.state = ProtocolState.HELO;
    }
    
    private void signalFailedDelivery(final Exception ex) {
        DeliveryRequest request = this.sessionState.getRequest();
        this.sessionState.reset(null);
        this.handler.failed(request, ex, this.context);
        if (this.log.isDebugEnabled()) {
            this.log.debug("Delivery failed: " + request, ex);
        }
    }
    
    private void signalFailedDelivery() {
        DeliveryRequest request = this.sessionState.getRequest();
        DeliveryResult result = new DeliveryResultImpl(
                this.sessionState.getReply(), 
                this.sessionState.getFailures());
        this.sessionState.reset(null);
        this.handler.failed(request, result, this.context);
        if (this.log.isDebugEnabled()) {
            this.log.debug("Delivery failed: " + request + "; result: " + result);
        }
    }
    
    private void signalSuccessfulDelivery() {
        DeliveryRequest request = this.sessionState.getRequest();
        DeliveryResult result = new DeliveryResultImpl(
                this.sessionState.getReply(), 
                this.sessionState.getFailures());
        this.sessionState.reset(null);
        this.handler.failed(request, result, this.context);
        if (this.log.isDebugEnabled()) {
            this.log.debug("Delivery succeeded: " + request + "; result: " + result);
        }
    }
    
    public void consumeData() {
        try {
            doConsumeData();
        } catch (IOException ex) {
            signalFailedDelivery(ex);
        } catch (SMTPProtocolException ex) {
            this.state = ProtocolState.QUIT;
            signalFailedDelivery(ex);
        }
    }
    
    public void produceData() {
        try {
            doProduceData();
        } catch (IOException ex) {
            signalFailedDelivery(ex);
        } catch (SMTPProtocolException ex) {
            this.state = ProtocolState.QUIT;
            signalFailedDelivery(ex);
        }
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
        if (this.currentCodec.isCompleted()) {
            SMTPReply reply = this.sessionState.getReply();
            
            if (this.log.isDebugEnabled()) {
                this.log.debug(this.state + " codec completed with reply: " + reply);
            }
            
            if (reply.getCode() == SMTPCodes.OK) {
                if (this.state == ProtocolState.DATA) {
                    signalSuccessfulDelivery();
                }
            } else {
                signalFailedDelivery();
            }
        }
        
        String nextCodec = this.currentCodec.next(this.codecs, this.sessionState);
        if (nextCodec != null) {
            this.state = ProtocolState.valueOf(nextCodec);
            this.currentCodec = this.codecs.getCodec(nextCodec);
            this.currentCodec.reset(this.iosession, this.sessionState);

            if (this.log.isDebugEnabled()) {
                this.log.debug("Next codec: " + this.state);
            }
            
            if (this.state == ProtocolState.MAIL) {
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
                    if (this.log.isDebugEnabled()) {
                        this.log.debug("Delivery request submitted: " + request);
                    }
                }
            }
        }
    }
    
    public void timeout() {
        try {
            doTimeout();
        } catch (IOException ex) {
            this.iosession.close();
        } catch (SMTPProtocolException ex) {
            this.iosession.close();
        }
    }
    
    private void doTimeout() throws IOException, SMTPProtocolException {
        if (this.state != ProtocolState.QUIT && this.currentCodec.isIdle()) {
            this.currentCodec = this.codecs.getCodec(ProtocolState.QUIT.name());
            this.currentCodec.reset(this.iosession, this.sessionState);
            this.state = ProtocolState.QUIT;
        } else {
            this.iosession.close();
        }
    }

}
