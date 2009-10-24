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
package com.ok2c.lightmtp.impl.protocol.cmd;

import junit.framework.Assert;

import org.junit.Test;

import com.ok2c.lightmtp.SMTPProtocolException;

public class TestAddressArgParser {

    @Test
    public void testBasicParsing() throws Exception {
        AddressArgParser argParser = new AddressArgParser("FROM:");
        String sender = argParser.parse("From:<me>");
        Assert.assertEquals("me", sender);
    }

    @Test
    public void testParsingEmptyAddress() throws Exception {
        AddressArgParser argParser = new AddressArgParser("FROM:");
        String sender = argParser.parse("From:<>");
        Assert.assertEquals("", sender);
    }

    @Test
    public void testLenientParsing() throws Exception {
        AddressArgParser argParser = new AddressArgParser("FROM:");
        String sender = argParser.parse("From:  <me>  ");
        Assert.assertEquals("me", sender);
    }

    @Test(expected=SMTPProtocolException.class)
    public void testParsingNull() throws Exception {
        AddressArgParser argParser = new AddressArgParser("FROM:");
        argParser.parse(null);
    }

    @Test(expected=SMTPProtocolException.class)
    public void testParsingMalformed() throws Exception {
        AddressArgParser argParser = new AddressArgParser("FROM:");
        argParser.parse("FROM<me>");
    }

    @Test(expected=SMTPProtocolException.class)
    public void testParsingMalformed2() throws Exception {
        AddressArgParser argParser = new AddressArgParser("FROM:");
        argParser.parse("Yo");
    }

    @Test(expected=SMTPProtocolException.class)
    public void testParsingMalformed3() throws Exception {
        AddressArgParser argParser = new AddressArgParser("FROM:");
        argParser.parse("FROM:<me");
    }

    @Test(expected=SMTPProtocolException.class)
    public void testParsingMalformed4() throws Exception {
        AddressArgParser argParser = new AddressArgParser("FROM:");
        argParser.parse("FROM:me>");
    }
}
