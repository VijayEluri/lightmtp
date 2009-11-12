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
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.ok2c.lightmtp.SMTPCode;
import com.ok2c.lightmtp.SMTPCodes;
import com.ok2c.lightmtp.SMTPProtocolException;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.message.SMTPContent;
import com.ok2c.lightmtp.message.SMTPMessageWriter;
import com.ok2c.lightmtp.message.SMTPReplyWriter;
import com.ok2c.lightmtp.message.content.FileSource;
import com.ok2c.lightmtp.message.content.FileStore;
import com.ok2c.lightmtp.protocol.BasicDeliveryRequest;
import com.ok2c.lightmtp.protocol.DeliveryHandler;
import com.ok2c.lightmtp.protocol.DeliveryRequest;
import com.ok2c.lightmtp.protocol.DeliveryResult;
import com.ok2c.lightmtp.protocol.ProtocolCodec;
import com.ok2c.lightmtp.protocol.ProtocolCodecs;
import com.ok2c.lightmtp.protocol.RcptResult;
import com.ok2c.lightnio.IOSession;
import com.ok2c.lightnio.SessionInputBuffer;
import com.ok2c.lightnio.SessionOutputBuffer;
import com.ok2c.lightnio.buffer.CharArrayBuffer;
import com.ok2c.lightnio.concurrent.BasicFuture;

public class ReceiveDataCodec implements ProtocolCodec<ServerState> {

    private final static int BUF_SIZE = 8 * 1024;
    private final static int LINE_SIZE = 1 * 1024;

    private final SMTPBuffers iobuffers;
    private final DeliveryHandler handler;
    private final File workingDir;
    private final DataAckMode mode;
    private final SMTPMessageWriter<SMTPReply> writer;
    private final LinkedList<SMTPReply> pendingReplies;
    private final CharArrayBuffer lineBuf;
    private final SMTPOutputBuffer contentBuf;
    
    private File tempFile;
    private FileStore fileStore;
    private boolean dataReceived;
    private BasicFuture<DeliveryResult> deliveryFuture;
    private boolean completed;

    public ReceiveDataCodec(
            final SMTPBuffers iobuffers, 
            final File workingDir, 
            final DeliveryHandler handler,
            final DataAckMode mode) {
        super();
        if (iobuffers == null) {
            throw new IllegalArgumentException("IO buffers may not be null");
        }
        if (workingDir == null) {
            throw new IllegalArgumentException("Working directory may not be null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Devliry handler may not be null");
        }
        this.iobuffers = iobuffers;
        this.workingDir = workingDir;
        this.handler = handler;
        this.mode = mode != null ? mode : DataAckMode.SINGLE;
        this.writer = new SMTPReplyWriter(true);
        this.pendingReplies = new LinkedList<SMTPReply>();       
        this.lineBuf = new CharArrayBuffer(LINE_SIZE);
        this.contentBuf = new SMTPOutputBuffer(BUF_SIZE, LINE_SIZE);
        
        this.dataReceived = false;
        this.deliveryFuture = null;
        this.completed = false;
    }

    public ReceiveDataCodec(
            final SMTPBuffers iobuffers, 
            final File workingDir, 
            final DeliveryHandler handler) {
        this(iobuffers, workingDir, handler, DataAckMode.SINGLE);
    }
    
    @Override
    protected void finalize() throws Throwable {
        cleanUp();
        super.finalize();
    }

    public void reset(
            final IOSession iosession,
            final ServerState sessionState) throws IOException, SMTPProtocolException {
        if (iosession == null) {
            throw new IllegalArgumentException("IO session may not be null");
        }
        if (sessionState == null) {
            throw new IllegalArgumentException("Session state may not be null");
        }

        cleanUp();

        if (!this.workingDir.exists()) {
            throw new IOException("Invalid working directory '" +
                    this.workingDir + "': directory does not exist");
        }
        if (!this.workingDir.canWrite()) {
            throw new IOException("Invalid working directory '" +
                    this.workingDir + "': directory is not writable");
        }
        this.tempFile = File.createTempFile("incoming-", ".email", this.workingDir);
        this.fileStore = new FileStore(this.tempFile);
        this.lineBuf.clear();

        this.pendingReplies.clear();        
        this.dataReceived = false;
        this.deliveryFuture = null;

        this.completed = false;
    }

    public void cleanUp() {
        if (this.fileStore != null) {
            this.fileStore.finish();
            this.fileStore = null;
        }
        if (this.tempFile != null) {
            this.tempFile.delete();
            this.tempFile = null;
        }
    }

    public void produceData(
            final IOSession iosession,
            final ServerState sessionState) throws IOException, SMTPProtocolException {
        if (iosession == null) {
            throw new IllegalArgumentException("IO session may not be null");
        }
        if (sessionState == null) {
            throw new IllegalArgumentException("Session state may not be null");
        }

        SessionOutputBuffer buf = this.iobuffers.getOutbuf();

        if (this.deliveryFuture != null) {
            synchronized (iosession) {
                if (this.deliveryFuture.isDone()) {
                    deliveryCompleted(sessionState);
                } else {
                    iosession.clearEvent(SelectionKey.OP_WRITE);
                }
            }
            while (!this.pendingReplies.isEmpty()) {
                this.writer.write(this.pendingReplies.removeFirst(), buf);
            }
        }

        if (buf.hasData()) {
            buf.flush(iosession.channel());
        }
        if (!buf.hasData()) {
            if (sessionState.getDataType() != null) {
                this.completed = true;
                sessionState.reset();
            }
            iosession.clearEvent(SelectionKey.OP_WRITE);
        }
    }

    private void deliveryCompleted(final ServerState sessionState) {
        if (this.mode.equals(DataAckMode.SINGLE)) {
            try {
                DeliveryResult result = this.deliveryFuture.get();
                this.pendingReplies.add(result.getReply());
            } catch (ExecutionException ex) {
                Throwable cause = ex.getCause();
                if (cause == null) {
                    cause = ex;
                }
                this.pendingReplies.add(createErrorReply(cause));
            } catch (InterruptedException ex) {
                this.pendingReplies.add(createErrorReply(ex));
            }
        } else {
            List<String> recipients = sessionState.getRecipients();
            try {
                DeliveryResult results = this.deliveryFuture.get();
                Map<String, SMTPReply> map = new HashMap<String, SMTPReply>();
                for (RcptResult res: results.getFailures()) {
                    map.put(res.getRecipient(), res.getReply());
                }
                for (String recipient: recipients) {
                    SMTPReply reply = map.get(recipient);
                    if (reply == null) {
                        reply = results.getReply();
                    }
                    this.pendingReplies.add(reply);
                }
            } catch (InterruptedException ex) {
                SMTPReply reply = createErrorReply(ex);
                for (int i = 0; i < recipients.size(); i++) {
                    this.pendingReplies.add(reply);
                }
            } catch (ExecutionException ex) {
                Throwable cause = ex.getCause();
                if (cause == null) {
                    cause = ex;
                }
                SMTPReply reply = createErrorReply(cause);
                for (int i = 0; i < recipients.size(); i++) {
                    this.pendingReplies.add(reply);
                }
            }
        }
    }

    private SMTPReply createErrorReply(final Throwable ex) {
        if (ex instanceof IOException) {
            return new SMTPReply(SMTPCodes.ERR_TRANS_PROCESSING_ERROR, 
                    new SMTPCode(4, 2, 0), ex.getMessage());
        } else if (ex instanceof InterruptedException) {
            return new SMTPReply(SMTPCodes.ERR_TRANS_PROCESSING_ERROR, 
                    new SMTPCode(4, 2, 0), ex.getMessage());
        } else {
            return new SMTPReply(SMTPCodes.ERR_PERM_TRX_FAILED,
                    new SMTPCode(5, 2, 0), ex.getMessage());
        }
    }
    
    public void consumeData(
            final IOSession iosession,
            final ServerState sessionState) throws IOException, SMTPProtocolException {
        if (iosession == null) {
            throw new IllegalArgumentException("IO session may not be null");
        }
        if (sessionState == null) {
            throw new IllegalArgumentException("Session state may not be null");
        }

        SessionInputBuffer buf = this.iobuffers.getInbuf();

        boolean hasData = true;
        while (hasData && !this.dataReceived) {
            int bytesRead = buf.fill(iosession.channel());
            if (buf.readLine(this.lineBuf, bytesRead == -1)) {

                processLine();

                if (!this.dataReceived) {
                    this.contentBuf.writeLine(this.lineBuf);
                }
                this.lineBuf.clear();
            } else {
                hasData = false;
            }
            if (this.dataReceived || this.contentBuf.length() > 4 * 1024 || bytesRead == -1) {
                this.contentBuf.flush(this.fileStore.channel());
            }
            if (bytesRead == -1) {
                throw new UnexpectedEndOfStreamException();
            }
        }
        if (this.contentBuf.hasData()) {
            this.contentBuf.flush(this.fileStore.channel());
        }
        if (this.dataReceived) {
            this.fileStore.finish();

            File file = this.fileStore.getFile();
            SMTPContent<ReadableByteChannel> content = new FileSource(file);
            DeliveryRequest deliveryRequest = new BasicDeliveryRequest(
                    sessionState.getSender(),
                    sessionState.getRecipients(),
                    content);

            this.deliveryFuture = new BasicFuture<DeliveryResult>(
                    new SessionResume<DeliveryResult>(iosession)); 
            this.handler.handle(deliveryRequest, this.deliveryFuture);
        }
    }

    private void processLine() {
        int lineLen = this.lineBuf.length();
        if (lineLen == 1) {
            if (this.lineBuf.charAt(0) == '.') {
                this.dataReceived = true;
            }
        } else if (lineLen > 1){
            // Strip away extra dot
            if (this.lineBuf.charAt(0) == '.' && this.lineBuf.charAt(1) == '.') {
                char[] buf = this.lineBuf.buffer();
                System.arraycopy(buf, 1, buf, 0, lineLen - 1);
                this.lineBuf.setLength(lineLen - 1);
            }
        }
    }

    public boolean isIdle() {
        return this.completed;
    }

    public boolean isCompleted() {
        return this.completed;
    }

    public String next(
            final ProtocolCodecs<ServerState> codecs,
            final ServerState sessionState) {
        if (isCompleted()) {
            if (sessionState.isTerminated()) {
                return ProtocolState.QUIT.name();
            }
            return ProtocolState.MAIL.name();
        } else {
            return null;
        }
    }

}
