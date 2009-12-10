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
import java.text.ParseException;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

public class TestInetAddressRangeParser {

    @Test
    public void testBasicInetAddressRangeParsing() throws Exception {
        InetAddressRangeParser parser = new InetAddressRangeParser();
        InetAddressRange range = parser.parse("127.0.0.0/8");
        Assert.assertNotNull(range);
        Assert.assertEquals(InetAddress.getByName("127.0.0.0"), range.getAddress());
        Assert.assertEquals(8, range.getMask());
    }

    @Test
    public void testInetAddressRangeParsingLotsBlanks() throws Exception {
        InetAddressRangeParser parser = new InetAddressRangeParser();
        InetAddressRange range = parser.parse("  127.0.0.0   /  8   ");
        Assert.assertNotNull(range);
        Assert.assertEquals(InetAddress.getByName("127.0.0.0"), range.getAddress());
        Assert.assertEquals(8, range.getMask());
    }

    @Test
    public void testInetAddressRangeOneHostname() throws Exception {
        InetAddressRangeParser parser = new InetAddressRangeParser();
        InetAddressRange range = parser.parse("  localhost   ");
        Assert.assertNotNull(range);
        Assert.assertEquals(InetAddress.getByName("localhost"), range.getAddress());
        Assert.assertEquals(0, range.getMask());
    }
    
    @Test(expected=ParseException.class)
    public void testInvalidMaskParsing() throws Exception {
        InetAddressRangeParser parser = new InetAddressRangeParser();
        parser.parse("127.0.0.0/ oopsie");
    }

    @Test(expected=ParseException.class)
    public void testNegativeMaskParsing() throws Exception {
        InetAddressRangeParser parser = new InetAddressRangeParser();
        parser.parse("127.0.0.0/-1");
    }

    @Test
    public void testBasicInetAddressRangeListParsing() throws Exception {
        InetAddressRangeParser parser = new InetAddressRangeParser();
        List<InetAddressRange> ranges = parser.parseAll("127.0.0.0/8, 10.0.0.0/16");
        Assert.assertNotNull(ranges);
        Assert.assertEquals(2, ranges.size());
        InetAddressRange r1 = ranges.get(0);
        InetAddressRange r2 = ranges.get(1);
        Assert.assertEquals(InetAddress.getByName("127.0.0.0"), r1.getAddress());
        Assert.assertEquals(8, r1.getMask());
        Assert.assertEquals(InetAddress.getByName("10.0.0.0"), r2.getAddress());
        Assert.assertEquals(16, r2.getMask());
    }

    @Test
    public void testEmptyInetAddressRangeListParsing() throws Exception {
        InetAddressRangeParser parser = new InetAddressRangeParser();
        List<InetAddressRange> ranges = parser.parseAll("");
        Assert.assertNotNull(ranges);
        Assert.assertEquals(0, ranges.size());
    }

}
