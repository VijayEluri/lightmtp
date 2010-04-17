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

import java.net.InetAddress;
import java.net.UnknownHostException;

import junit.framework.Assert;

import org.junit.Test;

public class TestInetAddressRange {

    private static byte[] createIP(int n1, int n2, int n3, int n4) {
        byte[] ip = new byte[4];
        ip[0] = (byte) n1;
        ip[1] = (byte) n2;
        ip[2] = (byte) n3;
        ip[3] = (byte) n4;
        return ip;
    }

    private static InetAddress createAddressByIP(int n1, int n2, int n3, int n4) throws UnknownHostException {
        return InetAddress.getByAddress(createIP(n1, n2, n3, n4));
    }

    @Test
    public void testConstructor() throws Exception {
        InetAddressRange range = new InetAddressRange(createAddressByIP(127, 0, 0, 254), 8);
        Assert.assertEquals(InetAddress.getByName("127.0.0.254"), range.getAddress());
        Assert.assertEquals(8, range.getMask());
        Assert.assertEquals("127.0.0.254/8", range.toString());
    }

    @Test
    public void testHashCode() throws Exception {
        InetAddressRange range1 = new InetAddressRange(createAddressByIP(127, 0, 0, 0), 8);
        InetAddressRange range2 = new InetAddressRange(createAddressByIP(127, 0, 0, 0), 16);
        InetAddressRange range3 = new InetAddressRange(createAddressByIP(10, 0, 0, 0), 16);
        InetAddressRange range4 = new InetAddressRange(createAddressByIP(127, 0, 0, 0), 8);
        Assert.assertTrue(range1.hashCode() == range1.hashCode());
        Assert.assertTrue(range1.hashCode() != range2.hashCode());
        Assert.assertTrue(range1.hashCode() != range3.hashCode());
        Assert.assertTrue(range2.hashCode() != range3.hashCode());
        Assert.assertTrue(range1.hashCode() == range4.hashCode());
    }

    @Test
    public void testEquals() throws Exception {
        InetAddressRange range1 = new InetAddressRange(createAddressByIP(127, 0, 0, 0), 8);
        InetAddressRange range2 = new InetAddressRange(createAddressByIP(127, 0, 0, 0), 16);
        InetAddressRange range3 = new InetAddressRange(createAddressByIP(10, 0, 0, 0), 16);
        InetAddressRange range4 = new InetAddressRange(createAddressByIP(127, 0, 0, 0), 8);
        Assert.assertTrue(range1.equals(range1));
        Assert.assertFalse(range1.equals(range2));
        Assert.assertFalse(range1.equals(range3));
        Assert.assertFalse(range2.equals(range3));
        Assert.assertTrue(range1.equals(range4));
    }

}
