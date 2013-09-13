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

import org.junit.Assert;
import org.junit.Test;

public class TestSMTPCode {

    @Test
    public void testConstructor() {
        try {
            new SMTPCode(-1, 1, 1);
            Assert.fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException expected) {
        }
        try {
            new SMTPCode(1, -1, 1);
            Assert.fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException expected) {
        }
        try {
            new SMTPCode(1, 1, -1);
            Assert.fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException expected) {
        }
        SMTPCode code = new SMTPCode(2, 4, 5);
        Assert.assertEquals(2, code.getCodeClass());
        Assert.assertEquals(4, code.getSubject());
        Assert.assertEquals(5, code.getDetail());
    }

    @Test
    public void testHashCode() {
        SMTPCode c1 = new SMTPCode(2, 4, 5);
        SMTPCode c2 = new SMTPCode(2, 3, 5);
        SMTPCode c3 = new SMTPCode(2, 4, 7);
        SMTPCode c4 = new SMTPCode(3, 4, 5);
        SMTPCode c5 = new SMTPCode(2, 4, 5);
        Assert.assertTrue(c1.hashCode() == c1.hashCode());
        Assert.assertFalse(c1.hashCode() == c2.hashCode());
        Assert.assertFalse(c1.hashCode() == c3.hashCode());
        Assert.assertFalse(c1.hashCode() == c4.hashCode());
        Assert.assertTrue(c1.hashCode() == c5.hashCode());
    }

    @Test
    public void testEqualsObject() {
        SMTPCode c1 = new SMTPCode(2, 4, 5);
        SMTPCode c2 = new SMTPCode(2, 3, 5);
        SMTPCode c3 = new SMTPCode(2, 4, 7);
        SMTPCode c4 = new SMTPCode(3, 4, 5);
        SMTPCode c5 = new SMTPCode(2, 4, 5);
        Assert.assertTrue(c1.equals(c1));
        Assert.assertFalse(c1.equals(Boolean.TRUE));
        Assert.assertFalse(c1.equals(null));
        Assert.assertFalse(c1.equals(c2));
        Assert.assertFalse(c1.equals(c3));
        Assert.assertFalse(c1.equals(c4));
        Assert.assertTrue(c1.equals(c5));
    }

}
