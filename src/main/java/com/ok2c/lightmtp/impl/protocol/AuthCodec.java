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
import java.nio.charset.Charset;
import java.util.Iterator;

import org.apache.commons.codec.binary.Base64;

import com.ok2c.lightmtp.SMTPCodes;
import com.ok2c.lightmtp.SMTPCommand;
import com.ok2c.lightmtp.SMTPConsts;
import com.ok2c.lightmtp.SMTPProtocolException;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.message.SMTPCommandWriter;
import com.ok2c.lightmtp.message.SMTPMessageParser;
import com.ok2c.lightmtp.message.SMTPMessageWriter;
import com.ok2c.lightmtp.message.SMTPReplyParser;
import com.ok2c.lightmtp.protocol.ProtocolCodec;
import com.ok2c.lightmtp.protocol.ProtocolCodecs;
import com.ok2c.lightnio.IOSession;
import com.ok2c.lightnio.SessionInputBuffer;
import com.ok2c.lightnio.SessionOutputBuffer;
import com.ok2c.lightnio.buffer.CharArrayBuffer;

/**
 * {@link ProtocolCodec} implementation which handles SMTP AUTH. See {@link AuthMode} for all supported modes
 *
 */
public class AuthCodec implements ProtocolCodec<ClientState> {

   enum CodecState {
       COMPLETED, 
       AUTH_READY, 
       AUTH_RESPONSE_READY,
       AUTH_PLAIN_INPUT_READY, 
       AUTH_PLAIN_INPUT_RESPONSE_EXPECTED,
       AUTH_LOGIN_USERNAME_INPUT_READY,
       AUTH_LOGIN_USERNAME_INPUT_RESPONSE_EXPECTED,
       AUTH_LOGIN_PASSWORD_INPUT_READY, 
       AUTH_LOGIN_PASSWORD_INPUT_RESPONSE_EXPECTED,
   }


   /**
    * Auth types which are supported
    *
    */
   enum AuthMode {
       PLAIN, LOGIN
   }

   private final static Charset AUTH_CHARSET = Charset.forName("UTF-8");
   private final static String AUTH_TYPE = "smtp.auth-type";
   private final SMTPBuffers iobuffers;
   private final SMTPMessageParser<SMTPReply> parser;
   private final SMTPMessageWriter<SMTPCommand> writer;

   private CodecState codecState;
   private final String username;
   private final String password;
   private final CharArrayBuffer lineBuf;

   public AuthCodec(final SMTPBuffers iobuffers, final String username, final String password) {
       super();
       if (iobuffers == null) {
           throw new IllegalArgumentException("IO buffers may not be null");
       }
       this.iobuffers = iobuffers;
       this.parser = new SMTPReplyParser();
       this.writer = new SMTPCommandWriter();
       this.username = username;
       this.password = password;
       this.codecState = CodecState.AUTH_READY;
       this.lineBuf = new CharArrayBuffer(1024);
   }

   /**
    * Return the AuthMode to use
    * 
    * @param types
    * @return type to use or null if no supported could be found
    */
   private AuthMode getAuthMode(String types) {
       String[] parts = types.split(" ");
       for (int i = 0; i < parts.length; i++) {
           if (parts[i].equals(AuthMode.LOGIN.name())) {
               return AuthMode.LOGIN;
           } else if (parts[i].equals(AuthMode.PLAIN.name())) {
               return AuthMode.PLAIN;
           }
       }
       return null;
   }

   /*
    * (non-Javadoc)
    * @see com.ok2c.lightmtp.protocol.ProtocolCodec#reset(com.ok2c.lightnio.IOSession, java.lang.Object)
    */
   public void reset(IOSession iosession, ClientState state)
           throws IOException, SMTPProtocolException {
       this.parser.reset();
       this.writer.reset();
       this.codecState = CodecState.AUTH_READY;
       this.iobuffers.setInputCharset(SMTPConsts.ASCII);
       this.lineBuf.clear();
       iosession.setEvent(SelectionKey.OP_WRITE);

   }

   /*
    * (non-Javadoc)
    * @see com.ok2c.lightmtp.protocol.ProtocolCodec#produceData(com.ok2c.lightnio.IOSession, java.lang.Object)
    */
   public void produceData(IOSession iosession, ClientState state)
           throws IOException, SMTPProtocolException {
       if (iosession == null) {
           throw new IllegalArgumentException("IO session may not be null");
       }
       if (state == null) {
           throw new IllegalArgumentException("Session state may not be null");
       }

       SessionOutputBuffer buf = this.iobuffers.getOutbuf();


       switch (this.codecState) {
       case AUTH_READY:
           AuthMode mode = null;
           Iterator<String> extensions = state.getExtensions().iterator();
           while(extensions.hasNext()) {
               String extension = extensions.next();
               if (extension.startsWith(ProtocolState.AUTH.name()) ) {
                   String types = extension.substring(ProtocolState.AUTH.name().length() + 1 );
                   mode  = getAuthMode(types);
                   if (mode != null) {
                       break;
                   }
               }
           }
           if (mode == null) {
               // TODO: Maybe we should just skip auth then and call the next codec in the chain
               new SMTPProtocolException("Unsupported AUTH types");
           } else {
               iosession.setAttribute(AUTH_TYPE, mode);
           }
           
           SMTPCommand auth = new SMTPCommand("AUTH", mode.name());
           this.writer.write(auth, buf);
           this.codecState = CodecState.AUTH_RESPONSE_READY;
           break;
           
       case AUTH_PLAIN_INPUT_READY:
           byte[] authdata = Base64.encodeBase64(("\0" + username + "\0" + password).getBytes(AUTH_CHARSET));
           lineBuf.append(authdata, 0 , authdata.length);
           this.codecState = CodecState.AUTH_PLAIN_INPUT_RESPONSE_EXPECTED;
           break;

       case AUTH_LOGIN_USERNAME_INPUT_READY:
           byte[] authUserData = Base64.encodeBase64(username.getBytes(AUTH_CHARSET));
           lineBuf.append(authUserData, 0, authUserData.length);

           this.codecState = CodecState.AUTH_LOGIN_USERNAME_INPUT_RESPONSE_EXPECTED;
           break;
       
       case AUTH_LOGIN_PASSWORD_INPUT_READY:
           byte[] authPassData = Base64.encodeBase64(password.getBytes(AUTH_CHARSET));
           lineBuf.append(authPassData,0, authPassData.length);

           this.codecState = CodecState.AUTH_LOGIN_PASSWORD_INPUT_RESPONSE_EXPECTED;
           break;
       }
       

       if (lineBuf.isEmpty() == false) {
           buf.writeLine(lineBuf);
           lineBuf.clear();
       }
       if (buf.hasData()) {
           buf.flush(iosession.channel());
       }
       if (!buf.hasData()) {
           iosession.clearEvent(SelectionKey.OP_WRITE);
       }
   }

   /*
    * (non-Javadoc)
    * @see com.ok2c.lightmtp.protocol.ProtocolCodec#consumeData(com.ok2c.lightnio.IOSession, java.lang.Object)
    */
   public void consumeData(IOSession iosession, ClientState state)
           throws IOException, SMTPProtocolException {
       if (iosession == null) {
           throw new IllegalArgumentException("IO session may not be null");
       }
       if (state == null) {
           throw new IllegalArgumentException("Session state may not be null");
       }

       SessionInputBuffer buf = this.iobuffers.getInbuf();

       int bytesRead = buf.fill(iosession.channel());
       SMTPReply reply = this.parser.parse(buf, bytesRead == -1);

       if (reply != null) {

        switch (this.codecState) {
            case AUTH_RESPONSE_READY:
                AuthMode mode = (AuthMode) iosession.getAttribute(AUTH_TYPE);
               
                if (reply.getCode() == SMTPCodes.START_AUTH_INPUT) {
                    if (mode == AuthMode.PLAIN) {
                        this.codecState = CodecState.AUTH_PLAIN_INPUT_READY;
                    } else if (mode == AuthMode.LOGIN) {
                        this.codecState = CodecState.AUTH_LOGIN_USERNAME_INPUT_READY;
                    }
                    state.setReply(reply);
                    iosession.setEvent(SelectionKey.OP_WRITE);
                } else {
                    // TODO: should we set the failure here ?
                    //       At the moment we just process as maybe its possible to send
                    //       the mail even without auth
                    this.codecState = CodecState.COMPLETED;
                    state.setReply(reply);
                }
                break;
                
           case AUTH_PLAIN_INPUT_RESPONSE_EXPECTED:

               if (reply.getCode() == SMTPCodes.AUTH_OK) {
                   this.codecState = CodecState.COMPLETED;
                   state.setReply(reply);
                   iosession.setEvent(SelectionKey.OP_WRITE);
               
               } else {
                   // TODO: should we set the failure here ?
                   //       At the moment we just process as maybe its possible to send
                   //       the mail even without auth
                   this.codecState = CodecState.COMPLETED;
                   state.setReply(reply);
               }
               break;
               
           case AUTH_LOGIN_USERNAME_INPUT_RESPONSE_EXPECTED:
               if (reply.getCode() == SMTPCodes.START_AUTH_INPUT) {
                   this.codecState = CodecState.AUTH_LOGIN_PASSWORD_INPUT_READY;
                   state.setReply(reply);
                   iosession.setEvent(SelectionKey.OP_WRITE);
               } else {
                   throw new SMTPProtocolException("Unexpected reply:" + reply);
               }
              
               break;
               
           case AUTH_LOGIN_PASSWORD_INPUT_RESPONSE_EXPECTED:
               if (reply.getCode() == SMTPCodes.AUTH_OK) {
                   this.codecState = CodecState.COMPLETED;
                   state.setReply(reply);
                   iosession.setEvent(SelectionKey.OP_WRITE);
               } else {
                   // TODO: should we set the failure here ?
                   //       At the moment we just process as maybe its possible to send
                   //       the mail even without auth
                   this.codecState = CodecState.COMPLETED;
                   state.setReply(reply);
                   
               }
               break;
               
           default:
               if (reply.getCode() == SMTPCodes.ERR_TRANS_SERVICE_NOT_AVAILABLE) {
                   state.setReply(reply);
                   this.codecState = CodecState.COMPLETED;
               } else {
                   throw new SMTPProtocolException("Unexpected reply:" + reply);
               }
           }
       } else {
           if (bytesRead == -1 && !state.isTerminated()) {
               throw new UnexpectedEndOfStreamException();
           }
       }
   }


   /*
    * (non-Javadoc)
    * @see com.ok2c.lightmtp.protocol.ProtocolCodec#isCompleted()
    */
   public boolean isCompleted() {
       return this.codecState == CodecState.COMPLETED;
   }
   
   /*
    * (non-Javadoc)
    * @see com.ok2c.lightmtp.protocol.ProtocolCodec#next(com.ok2c.lightmtp.protocol.ProtocolCodecs, java.lang.Object)
    */
   public String next(ProtocolCodecs<ClientState> codecs, ClientState state) {
       if (isCompleted()) {
           return ProtocolState.MAIL.name();
       } else {
           return null;
       }

   }

   /**
    * Nothing todo here
    */
   public void cleanUp() {
   }

}