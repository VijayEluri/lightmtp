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
package com.ok2c.lightmtp.message.content;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.WritableByteChannel;

import com.ok2c.lightmtp.message.SMTPContent;

public class FileStore implements SMTPContent<WritableByteChannel> {

    private final File file;

    private RandomAccessFile rafile;

    public FileStore(final File file) {
        super();
        this.file = file;
        this.rafile = null;
    }

    @Override
    protected void finalize() throws Throwable {
        reset();
        super.finalize();
    }

    public File getFile() {
        return this.file;
    }

    @Override
    public WritableByteChannel channel() throws FileNotFoundException {
        if (this.rafile == null) {
            this.rafile = new RandomAccessFile(this.file, "rw");
        }
        return this.rafile.getChannel();
    }

    @Override
    public long length() {
        return this.file.length();
    }

    @Override
    public void reset() {
        if (this.rafile != null) {
            try {
                this.rafile.close();
            } catch (IOException ignore) {
            }
        }
        this.rafile = null;
    }

}
