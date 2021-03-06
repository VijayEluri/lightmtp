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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.message.ParserCursor;
import org.apache.http.util.Args;
import org.apache.http.util.CharArrayBuffer;

public final class InetAddressRangeParser {

    private static final char[] COMMA = new char[] { ',' };

    private static boolean isOneOf(char c, char[] delimiters) {
        if (delimiters != null) {
            for (char delimiter: delimiters) {
                if (c == delimiter) {
                    return true;
                }
            }
        }
        return false;
    }

    public InetAddressRange parse(
            final CharArrayBuffer buffer,
            final ParserCursor cursor,
            final char[] delimiters) throws ParseException, UnknownHostException {

        Args.notNull(buffer, "Char array buffer");
        Args.notNull(cursor, "Parser cursor");

        int pos = cursor.getPos();
        int indexFrom = cursor.getPos();
        int indexTo = cursor.getUpperBound();

        while (pos < indexTo) {
            char ch = buffer.charAt(pos);
            if (ch == '/') {
                break;
            }
            if (isOneOf(ch, delimiters)) {
                break;
            }
            pos++;
        }

        InetAddress address = InetAddress.getByName(buffer.substringTrimmed(indexFrom, pos));
        int mask = 0;

        if (pos < indexTo && buffer.charAt(pos) == '/') {
            pos++;
            indexFrom = pos;
            while (pos < indexTo) {
                char ch = buffer.charAt(pos);
                if (isOneOf(ch, delimiters)) {
                    break;
                }
                pos++;
            }
            try {
                mask = Integer.parseInt(buffer.substringTrimmed(indexFrom, pos));
                if (mask < 0) {
                    throw new ParseException("Negative range mask", indexFrom);
                }
            } catch (NumberFormatException ex) {
                throw new ParseException("Invalid range mask", indexFrom);
            }
        }
        cursor.updatePos(pos);
        return new InetAddressRange(address, mask);
    }

    public InetAddressRange parse(
            final String s) throws ParseException, UnknownHostException {
        CharArrayBuffer buffer = new CharArrayBuffer(s.length());
        buffer.append(s);
        ParserCursor cursor = new ParserCursor(0, s.length());
        return parse(buffer, cursor, COMMA);
    }

    public List<InetAddressRange> parseAll(
            final CharArrayBuffer buffer,
            final ParserCursor cursor,
            final char[] delimiters) throws ParseException, UnknownHostException {

        Args.notNull(buffer, "Char array buffer");
        Args.notNull(cursor, "Parser cursor");

        char[] delims = delimiters != null ? delimiters : COMMA;

        List<InetAddressRange> ranges = new ArrayList<InetAddressRange>();
        while (!cursor.atEnd()) {
            ranges.add(parse(buffer, cursor, delims));
            int pos = cursor.getPos();
            if (pos < cursor.getUpperBound() && isOneOf(buffer.charAt(pos), delims)) {
                cursor.updatePos(pos + 1);
            }
        }
        return ranges;
    }

    public List<InetAddressRange> parseAll(
            final String s) throws ParseException, UnknownHostException {
        CharArrayBuffer buffer = new CharArrayBuffer(s.length());
        buffer.append(s);
        ParserCursor cursor = new ParserCursor(0, s.length());
        return parseAll(buffer, cursor, null);
    }

}
