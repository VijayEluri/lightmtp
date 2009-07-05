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
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

public class DNSUtils {

    public static String getLocalDomain(final SocketAddress address) {
        InetAddress inetAddress;
        if (address instanceof InetSocketAddress) {
            inetAddress = ((InetSocketAddress) address).getAddress();
        } else {
            try {
                inetAddress = InetAddress.getLocalHost();
            } catch (UnknownHostException ex) {
                inetAddress = null;
            }
        }
        String hostname = null;
        if (inetAddress != null) {
            hostname = inetAddress.getCanonicalHostName();
            int idx = hostname.indexOf('.');
            if (idx == -1) {
                hostname = null;
            } else {
                hostname = hostname.substring(idx + 1);
            }
        }
        if (hostname == null) {
            hostname = "localdomain";
        }
        return hostname; 
    }
    
}
