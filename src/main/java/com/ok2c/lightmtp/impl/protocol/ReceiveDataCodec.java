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
import java.nio.channels.SelectionKey;
import java.nio.charset.Charset;

import com.ok2c.lightmtp.SMTPCode;
import com.ok2c.lightmtp.SMTPCodes;
import com.ok2c.lightmtp.SMTPConsts;
import com.ok2c.lightmtp.SMTPProtocolException;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.message.SMTPMessageWriter;
import com.ok2c.lightmtp.message.SMTPReplyWriter;
import com.ok2c.lightmtp.message.content.FileStore;
import com.ok2c.lightmtp.protocol.ProtocolCodec;
import com.ok2c.lightmtp.protocol.ProtocolCodecs;
import com.ok2c.lightnio.IOSession;
import com.ok2c.lightnio.SessionInputBuffer;
import com.ok2c.lightnio.SessionOutputBuffer;
import com.ok2c.lightnio.buffer.CharArrayBuffer;
import com.ok2c.lightnio.impl.SessionOutputBufferImpl;

public class ReceiveDataCodec implements ProtocolCodec<ServerSessionState> {

    private final static int BUF_SIZE = 8 * 1024;
    private final static int LINE_SIZE = 1 * 1024;

    private static class ContentBuffer extends SessionOutputBufferImpl {

        ContentBuffer(final Charset charset) {
            super(BUF_SIZE, LINE_SIZE, charset);
        }

        public void clear() {
            super.clear();
        }

    }

    private final SMTPMessageWriter<SMTPReply> writer;
    private final File workingDir;
    private final CharArrayBuffer lineBuf;
    private final ContentBuffer contentBuf;

    private File tempFile;
    private FileStore fileStore;
    private boolean emptyLineFound;
    private boolean terminalDotFound;
    private SMTPReply pendingReply;
    private boolean completed;

    public ReceiveDataCodec(final File workingDir) {
        super();
        if (workingDir == null) {
            throw new IllegalArgumentException("Working directory may not be null");
        }
        this.writer = new SMTPReplyWriter();
        this.workingDir = workingDir;
        this.lineBuf = new CharArrayBuffer(LINE_SIZE);
        this.contentBuf = new ContentBuffer(SMTPConsts.ASCII);
        this.emptyLineFound = false;
        this.terminalDotFound = false;
        this.pendingReply = null;
        this.completed = false;
    }

    @Override
    protected void finalize() throws Throwable {
        cleanUp();
        super.finalize();
    }

    public void reset(
            final IOSession iosession,
            final ServerSessionState sessionState) throws IOException, SMTPProtocolException {
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

        this.emptyLineFound = false;
        this.terminalDotFound = false;
        this.pendingReply = null;

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
            final ServerSessionState sessionState) throws IOException, SMTPProtocolException {
        if (iosession == null) {
            throw new IllegalArgumentException("IO session may not be null");
        }
        if (sessionState == null) {
            throw new IllegalArgumentException("Session state may not be null");
        }

        SessionOutputBuffer buf = sessionState.getOutbuf();

        if (this.pendingReply != null) {
            this.writer.write(this.pendingReply, buf);
            this.pendingReply = null;
        }

        if (buf.hasData()) {
            buf.flush(iosession.channel());
        }
        if (!buf.hasData()) {
            if (sessionState.getDataType() != null) {
                this.completed = true;
            }
            iosession.clearEvent(SelectionKey.OP_WRITE);
        }
    }

    public void consumeData(
            final IOSession iosession,
            final ServerSessionState sessionState) throws IOException, SMTPProtocolException {
        if (iosession == null) {
            throw new IllegalArgumentException("IO session may not be null");
        }
        if (sessionState == null) {
            throw new IllegalArgumentException("Session state may not be null");
        }

        SessionInputBuffer buf = sessionState.getInbuf();

        boolean hasData = true;
        while (hasData && !this.terminalDotFound) {
            int bytesRead = buf.fill(iosession.channel());
            if (buf.readLine(this.lineBuf, bytesRead == -1)) {

                processLine();

                this.contentBuf.writeLine(this.lineBuf);
                this.lineBuf.clear();
            } else {
                hasData = false;
            }
            if (this.contentBuf.length() > 4 * 1024 || bytesRead == -1) {
                this.contentBuf.flush(this.fileStore.channel());
            }
            if (bytesRead == -1) {
                throw new UnexpectedEndOfStreamException();
            }
        }
        if (this.contentBuf.hasData()) {
            this.contentBuf.flush(this.fileStore.channel());
        }
        if (this.terminalDotFound) {
            this.fileStore.finish();

            iosession.setEvent(SelectionKey.OP_WRITE);

            SMTPCode enhancedCode = null;
            if (sessionState.isEnhancedCodeCapable()) {
                enhancedCode = new SMTPCode(2, 6, 0);
            }
            this.pendingReply = new SMTPReply(SMTPCodes.OK, enhancedCode, "message accepted");

            cleanUp();
        }
    }

    private void processLine() {
        boolean previousEmptyLine = this.emptyLineFound;
        int lineLen = this.lineBuf.length();
        if (lineLen == 0) {
            this.emptyLineFound = true;
        } else if (lineLen == 1) {
            if (previousEmptyLine && this.lineBuf.charAt(0) == '.') {
                this.terminalDotFound = true;
            }
        } else {
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
            final ProtocolCodecs<ServerSessionState> codecs,
            final ServerSessionState sessionState) {
        if (isCompleted()) {
            return ProtocolState.MAIL.name();
        } else {
            return null;
        }
    }

}