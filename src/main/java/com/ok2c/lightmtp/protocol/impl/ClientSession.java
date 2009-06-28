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
package com.ok2c.lightmtp.protocol.impl;

import java.io.IOException;
import java.nio.channels.SelectionKey;

import com.ok2c.lightmtp.SMTPCodes;
import com.ok2c.lightmtp.SMTPProtocolException;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.protocol.DeliveryCallback;
import com.ok2c.lightmtp.protocol.DeliveryJob;
import com.ok2c.lightmtp.protocol.DeliveryRequest;
import com.ok2c.lightmtp.protocol.ProtocolCodec;
import com.ok2c.lightmtp.protocol.ProtocolCodecs;
import com.ok2c.lightnio.IOSession;

public class ClientSession {
    
    private final IOSession iosession;
    private final SessionState sessionState;
    
    private ProtocolCodecs<SessionState> codecs;
    private ProtocolCodec<SessionState> currentCodec;
    
    private DeliveryJobImpl delivery;
    private ProtocolState state;
    
    public ClientSession(final IOSession iosession) {
        super();
        this.iosession = iosession;
        this.sessionState = new SessionState();
        this.iosession.setBufferStatus(this.sessionState);
        this.codecs = new ProtocolCodecRegistry<SessionState>();
        this.state = ProtocolState.INIT;
        
        this.codecs.register(ProtocolState.HELO.name(), new ExtendedHeloCodec());
        this.codecs.register(ProtocolState.MAIL.name(), new SimpleMailTrxCodec(false));
        this.codecs.register(ProtocolState.DATA.name(), new SendDataCodec(false));
        this.codecs.register(ProtocolState.QUIT.name(), new QuitCodec());
    }

    public DeliveryJob submit(final DeliveryRequest request, final DeliveryCallback callback) {
        if (request == null) {
            throw new IllegalArgumentException("Request may not be null");
        }
        if (this.delivery != null) {
            throw new IllegalStateException("Another request is already being processed");
        }
        this.delivery = new DeliveryJobImpl(request, callback);
        this.iosession.setEvent(SelectionKey.OP_WRITE);
        return this.delivery;
    }
    
    public void connected() throws IOException, SMTPProtocolException {
        if (this.state != ProtocolState.INIT) {
            throw new IllegalStateException("Unexpected state: " + this.state);
        }
        
        this.currentCodec = this.codecs.getCodec(ProtocolState.HELO.name());
        this.currentCodec.reset(this.iosession, this.sessionState);
        
        this.state = ProtocolState.HELO;
    }
    
    private void signalFailedDelivery(final Exception ex) {
        this.delivery.failed(ex);
        this.sessionState.reset(null);
    }
    
    private void signalFailedDelivery() {
        this.delivery.failed(new DeliveryResultImpl(
                this.sessionState.getReply(), 
                this.sessionState.getRcptFailures()));
        this.sessionState.reset(null);
    }
    
    private void signalSuccessfulDelivery() {
        this.delivery.failed(new DeliveryResultImpl(
                this.sessionState.getReply(), 
                this.sessionState.getRcptFailures()));
        this.sessionState.reset(null);
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
