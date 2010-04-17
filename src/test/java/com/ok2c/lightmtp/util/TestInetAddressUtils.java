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
package com.ok2c.lightmtp.util;

import org.junit.Test;

import junit.framework.Assert;

public class TestInetAddressUtils {

    @Test
    public void testValidIPv4Address() {
        Assert.assertTrue(InetAddressUtils.isIPv4Address("127.0.0.1"));
        Assert.assertTrue(InetAddressUtils.isIPv4Address("192.168.0.0"));
        Assert.assertTrue(InetAddressUtils.isIPv4Address("255.255.255.255"));
    }

    @Test
    public void testInvalidIPv4Address() {
        Assert.assertFalse(InetAddressUtils.isIPv4Address(" 127.0.0.1 "));  // Blanks not allowed
        Assert.assertFalse(InetAddressUtils.isIPv4Address("g.ar.ba.ge"));
        Assert.assertFalse(InetAddressUtils.isIPv4Address("192.168.0"));
        Assert.assertFalse(InetAddressUtils.isIPv4Address("256.255.255.255"));
    }

    @Test
    public void testValidIPv6Address() {
        Assert.assertTrue(InetAddressUtils.isIPv6StdAddress("2001:0db8:0000:0000:0000:0000:1428:57ab"));
        Assert.assertTrue(InetAddressUtils.isIPv6StdAddress("2001:db8:0:0:0:0:1428:57ab"));
        Assert.assertTrue(InetAddressUtils.isIPv6HexCompressedAddress("2001:0db8:0:0::1428:57ab"));
        Assert.assertTrue(InetAddressUtils.isIPv6HexCompressedAddress("2001:0db8::1428:57ab"));
        Assert.assertTrue(InetAddressUtils.isIPv6HexCompressedAddress("2001:db8::1428:57ab"));
    }

    @Test
    public void testInvalidIPv6Address() {
        Assert.assertFalse(InetAddressUtils.isIPv6Address("2001:0db8:0000:garb:age0:0000:1428:57ab"));
        Assert.assertFalse(InetAddressUtils.isIPv6Address("2001:0gb8:0000:0000:0000:0000:1428:57ab"));
    }

}
