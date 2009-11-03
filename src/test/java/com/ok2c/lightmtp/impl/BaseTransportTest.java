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

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;

import com.ok2c.lightmtp.impl.agent.DefaultMailTransferAgent;
import com.ok2c.lightmtp.impl.agent.DefaultMailUserAgent;
import com.ok2c.lightnio.IOReactorExceptionHandler;
import com.ok2c.lightnio.impl.IOReactorConfig;

public abstract class BaseTransportTest {

    protected DefaultMailTransferAgent mta;
    protected DefaultMailUserAgent mua;
    
    @Before
    public void setUp() throws Exception {
        File tmpDir = new File(System.getProperty("java.io.tmpdir", "."));
        
        IOReactorConfig config = new IOReactorConfig();
        config.setWorkerCount(2);
        this.mta = new DefaultMailTransferAgent("Test SMTP server", tmpDir, config);
        this.mta.setExceptionHandler(new IOReactorExceptionHandler() {
            
            public boolean handle(final RuntimeException ex) {
                ex.printStackTrace(System.out);
                return false;
            }
            
            public boolean handle(final IOException ex) {
                ex.printStackTrace(System.out);
                return false;
            }

        });
        
        this.mua = new DefaultMailUserAgent(config);
        this.mua.setExceptionHandler(new IOReactorExceptionHandler() {
            
            public boolean handle(final RuntimeException ex) {
                ex.printStackTrace(System.out);
                return false;
            }
            
            public boolean handle(final IOException ex) {
                ex.printStackTrace(System.out);
                return false;
            }

        });
    }
    
    @After
    public void tearDown() throws Exception {
        try {
            this.mua.shutdown();
        } catch (IOException ex) {
            ex.printStackTrace(System.out);
        }
        try {
            this.mta.shutdown();
        } catch (IOException ex) {
            ex.printStackTrace(System.out);
        }
    }

}
