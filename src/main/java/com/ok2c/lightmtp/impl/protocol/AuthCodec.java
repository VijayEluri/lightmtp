package com.ok2c.lightmtp.impl.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
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


   enum AuthMode {
       PLAIN, LOGIN
   }

   private final static String AUTH_TYPE = "smtp.auth-type";
   private final SMTPBuffers iobuffers;
   private final SMTPMessageParser<SMTPReply> parser;
   private final SMTPMessageWriter<SMTPCommand> writer;

   private CodecState codecState;
   private String username;
   private String password;

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
   }

   private AuthMode getAuthMode(String modeString) throws SMTPProtocolException{
       if (modeString.equals(AuthMode.LOGIN.name())) {
           return AuthMode.LOGIN;
       } else if (modeString.equals(AuthMode.PLAIN.name())) {
           return AuthMode.PLAIN;
       }
       throw null;
   }

   public void reset(IOSession iosession, ClientState state)
           throws IOException, SMTPProtocolException {
       this.parser.reset();
       this.writer.reset();
       this.codecState = CodecState.AUTH_READY;
       this.iobuffers.setInputCharset(SMTPConsts.ASCII);

       iosession.setEventMask(SelectionKey.OP_READ);
   }

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
           String type = null;
           AuthMode mode = null;
           Iterator<String> extensions = state.getExtensions().iterator();
           while(extensions.hasNext()) {
               String extension = extensions.next();
               if (extension.startsWith(ProtocolState.AUTH.name()) ) {
                   type = extension.substring(ProtocolState.AUTH.name().length() + 1 );
                   mode  = getAuthMode(type);
                   if (mode != null) {
                       iosession.setAttribute(AUTH_TYPE, mode);
                       break;
                   }
               }
           }
           if (mode == null) new SMTPProtocolException("Unsupported AUTH types");
           
           SMTPCommand auth = new SMTPCommand("AUTH", type);
           this.writer.write(auth, buf);
           this.codecState = CodecState.AUTH_RESPONSE_READY;
           iosession.setEventMask(SelectionKey.OP_READ);

           break;
       case AUTH_PLAIN_INPUT_READY:
           byte[] authdata = Base64.encodeBase64(("\0" + username + "\0" + password).getBytes(SMTPConsts.ASCII));
           buf.write(ByteBuffer.wrap(authdata));
           this.codecState = CodecState.AUTH_LOGIN_PASSWORD_INPUT_RESPONSE_EXPECTED;
           iosession.setEventMask(SelectionKey.OP_READ);
           break;

       case AUTH_LOGIN_USERNAME_INPUT_READY:
           byte[] authUserData = Base64.encodeBase64(username.getBytes(SMTPConsts.ASCII));
           buf.write(ByteBuffer.wrap(authUserData));
           this.codecState = CodecState.AUTH_LOGIN_PASSWORD_INPUT_RESPONSE_EXPECTED;
           iosession.setEventMask(SelectionKey.OP_READ);

       
       case AUTH_LOGIN_PASSWORD_INPUT_READY:
           byte[] authPassData = Base64.encodeBase64(password.getBytes(SMTPConsts.ASCII));
           buf.write(ByteBuffer.wrap(authPassData));
           this.codecState = CodecState.AUTH_LOGIN_PASSWORD_INPUT_RESPONSE_EXPECTED;
           iosession.setEventMask(SelectionKey.OP_READ);
       }
       

       if (buf.hasData()) {
           buf.flush(iosession.channel());
       }
       if (!buf.hasData()) {
           iosession.clearEvent(SelectionKey.OP_WRITE);
       }
   }

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
                    iosession.setEvent(SelectionKey.OP_WRITE);
                } else {
                    this.codecState = CodecState.COMPLETED;
                    state.setReply(reply);
                }
           case AUTH_PLAIN_INPUT_RESPONSE_EXPECTED:
               if (reply.getCode() == SMTPCodes.AUTH_OK) {
                   this.codecState = CodecState.COMPLETED;
                   state.setReply(reply);
                   iosession.setEvent(SelectionKey.OP_WRITE);
               
               } else {
                   this.codecState = CodecState.AUTH_READY;
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
                   iosession.setEvent(SelectionKey.OP_WRITE);
               } else {
                   this.codecState = CodecState.AUTH_READY;
                   state.setReply(reply);
               }
               
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


   public boolean isCompleted() {
       return this.codecState == CodecState.COMPLETED;
   }
   
   public String next(ProtocolCodecs<ClientState> codecs, ClientState state) {
       if (isCompleted()) {
           return ProtocolState.MAIL.name();
       } else {
           return null;
       }

   }

   public void cleanUp() {
   }

}