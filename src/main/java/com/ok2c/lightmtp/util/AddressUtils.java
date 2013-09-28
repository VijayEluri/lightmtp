/**
 *  Copyright 2005-2009 OK2 Consulting GmbH.
 */
package com.ok2c.lightmtp.util;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.List;

import org.apache.http.util.Args;

public final class AddressUtils {

    public static String getLocalCanonicalHostName() {
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException ignore) {
        }
        String hostname = null;
        if (inetAddress != null) {
            hostname = inetAddress.getCanonicalHostName();
        }
        if (hostname == null) {
            hostname = "localhost.localdomain";
        }
        return hostname;
    }

    public static String resolveLocalDomain(final SocketAddress address) {
        InetAddress inetAddress;
        if (address != null && address instanceof InetSocketAddress) {
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


    public static SocketAddress parseSocketAddress(final String s) throws ParseException {
        Args.notNull(s, "Text");
        int idx = s.indexOf(':');
        if (idx == -1) {
            throw new ParseException(s + " is not a valid socket address", 0);
        }
        String host = s.substring(0, idx);
        int port;
        try {
            port = Integer.parseInt(s.substring(idx + 1, s.length()));
        } catch (NumberFormatException ex) {
            throw new ParseException(s + " is not a valid socket address", idx + 1);
        }
        if (host.length() == 0) {
            return new InetSocketAddress(port);
        } else {
            return new InetSocketAddress(host, port);
        }
    }

    public static List<InetAddressRange> parseIPRange(
            final String s) throws ParseException, UnknownHostException {
        Args.notNull(s, "Text");
        InetAddressRangeParser parser = new InetAddressRangeParser();
        return parser.parseAll(s);
    }

}
