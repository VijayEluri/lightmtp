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

import java.nio.charset.Charset;

import org.apache.http.impl.nio.reactor.SessionOutputBufferImpl;

import com.ok2c.lightmtp.SMTPConsts;

public class SMTPOutputBuffer extends SessionOutputBufferImpl {

    public SMTPOutputBuffer(final int buffersize, final int linebuffersize, final Charset charset) {
        super(buffersize, linebuffersize, charset);
    }

    public SMTPOutputBuffer(final int buffersize, final int linebuffersize) {
        super(buffersize, linebuffersize, SMTPConsts.ASCII);
    }

    @Override
    public void clear() {
        super.clear();
    }

}
