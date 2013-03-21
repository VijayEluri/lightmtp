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

import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.junit.After;
import org.junit.Before;

import com.ok2c.lightmtp.impl.agent.DefaultMailClientTransport;
import com.ok2c.lightmtp.impl.agent.DefaultMailServerTransport;

public abstract class BaseTransportTest {

    protected static File TMP_DIR = new File(System.getProperty("java.io.tmpdir", "."));

    protected DefaultMailServerTransport mta;
    protected DefaultMailClientTransport mua;

    @Before
    public void setUp() throws Exception {

        IOReactorConfig config = IOReactorConfig.custom().setIoThreadCount(2).build();
        this.mta = new DefaultMailServerTransport(TMP_DIR, config);
        this.mta.setExceptionHandler(new BasicExceptionHandler());

        this.mua = new DefaultMailClientTransport(config);
        this.mua.setExceptionHandler(new BasicExceptionHandler());
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
