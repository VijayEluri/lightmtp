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

public final class InetAddressRange {

    private final InetAddress address;
    private final int mask;
    
    public InetAddressRange(final InetAddress address, int mask) {
        super();
        if (address == null) {
            throw new IllegalArgumentException("Address may not be null");
        }
        if (mask < 0) {
            throw new IllegalArgumentException("Address mask may not be negative");
        }
        this.address = address;
        this.mask = mask;
    }

    public InetAddress getAddress() {
        return this.address;
    }

    public int getMask() {
        return this.mask;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (obj instanceof InetAddressRange) {
            InetAddressRange that = (InetAddressRange) obj;
            return this.address.equals(that.address) 
                && this.mask == that.mask;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 37 + this.address.hashCode();
        hash = hash * 37 + this.mask;
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        if (this.mask > 0) {
            buffer.append(this.address.getHostAddress());
            buffer.append('/');
            buffer.append(this.mask);
            return buffer.toString();
        } else {
            return this.address.getHostName();
        }
    }
    
}