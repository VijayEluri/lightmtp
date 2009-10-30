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
package com.ok2c.lightmtp;

import junit.framework.Assert;

import org.junit.Test;

public class TestSMTPError {

    @Test
    public void testConstructor() {
        try {
            new SMTPErrorException(-1, null, "whatever");
            Assert.fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException expected) {
        }
        try {
            new SMTPErrorException(300, null, "whatever");
            Assert.fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException expected) {
        }
        try {
            new SMTPErrorException(600, null, "whatever");
            Assert.fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException expected) {
        }
        try {
            new SMTPErrorException(550, new SMTPCode(2, 2, 0), "whatever");
            Assert.fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException expected) {
        }
        try {
            new SMTPErrorException(500, new SMTPCode(6, 2, 0), "whatever");
            Assert.fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException expected) {
        }

        SMTPErrorException ex = new SMTPErrorException(550, new SMTPCode(5, 5, 4), "whatever");
        Assert.assertEquals(550, ex.getCode());
        Assert.assertEquals(new SMTPCode(5, 5, 4), ex.getEnhancedCode());

        SMTPErrorException ex2 = new SMTPErrorException(550, null, "whatever");
        Assert.assertEquals(550, ex2.getCode());
        Assert.assertEquals(null, ex2.getEnhancedCode());
    }

}
