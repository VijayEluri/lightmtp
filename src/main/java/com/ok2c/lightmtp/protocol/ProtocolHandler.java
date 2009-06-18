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
package com.ok2c.lightmtp.protocol;

import java.io.IOException;

import com.ok2c.lightmtp.SMTPProtocolException;
import com.ok2c.lightnio.IOSession;
import com.ok2c.lightnio.SessionInputBuffer;
import com.ok2c.lightnio.SessionOutputBuffer;

public interface ProtocolHandler<T> {

    void produceData(
            IOSession iosession, 
            SessionOutputBuffer buf) throws IOException, SMTPProtocolException;

    void comsumeData(
            IOSession iosession,
            SessionInputBuffer buf) throws IOException, SMTPProtocolException;

    boolean isCompleted();
    
    T getResult();
    
}
