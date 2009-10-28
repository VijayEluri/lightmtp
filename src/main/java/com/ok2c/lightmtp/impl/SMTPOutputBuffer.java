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
package com.ok2c.lightmtp.impl;

import com.ok2c.lightmtp.SMTPConsts;
import com.ok2c.lightnio.impl.SessionOutputBufferImpl;

public class SMTPOutputBuffer extends SessionOutputBufferImpl {

    public SMTPOutputBuffer(int buffersize, int linebuffersize) {
        super(buffersize, linebuffersize, SMTPConsts.ASCII);
    }

    public void clear() {
        super.clear();
    }

}
