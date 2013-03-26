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
package com.ok2c.lightmtp.message;

import java.nio.charset.CharacterCodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.message.ParserCursor;
import org.apache.http.nio.reactor.SessionInputBuffer;
import org.apache.http.util.Args;
import org.apache.http.util.CharArrayBuffer;

import com.ok2c.lightmtp.SMTPCode;
import com.ok2c.lightmtp.SMTPConsts;
import com.ok2c.lightmtp.SMTPProtocolException;
import com.ok2c.lightmtp.SMTPReply;

public class SMTPReplyParser implements SMTPMessageParser<SMTPReply> {

    private final CharArrayBuffer lineBuf;
    private final LinkedList<ParsedLine> parsedLines;
    private final int maxLineLen;
    private final boolean useEnhancedCodes;

    public SMTPReplyParser(final int maxLineLen, final boolean useEnhancedCodes) {
        super();
        this.lineBuf = new CharArrayBuffer(1024);
        this.parsedLines = new LinkedList<ParsedLine>();
        this.maxLineLen = maxLineLen;
        this.useEnhancedCodes = useEnhancedCodes;
    }

    public SMTPReplyParser(final boolean useEnhancedCodes) {
        this(SMTPConsts.MAX_REPLY_LEN, useEnhancedCodes);
    }

    public SMTPReplyParser() {
        this(false);
    }

    @Override
    public void reset() {
        this.parsedLines.clear();
        this.lineBuf.clear();
    }

    @Override
    public SMTPReply parse(
            final SessionInputBuffer buf, final boolean endOfStream) throws SMTPProtocolException {
        Args.notNull(buf, "Session input buffer");
        while (readLine(buf, endOfStream)) {
            ParsedLine current = parseLine();
            if (!this.parsedLines.isEmpty()) {
                ParsedLine previous = this.parsedLines.getLast();
                if (!sameCode(current, previous)) {
                    throw new SMTPProtocolException(
                            "Invalid multiline reply: status code mismatch");
                }
            }
            this.parsedLines.add(current);
            if (current.isTerminal()) {
                List<String> lines = new ArrayList<String>(this.parsedLines.size());
                for (ParsedLine parsedLine: this.parsedLines) {
                    lines.add(parsedLine.getText());
                }
                SMTPReply reply = new SMTPReply(
                        current.getCode(),
                        current.getEnhancedCode(),
                        lines);
                reset();
                return reply;
            }
        }
        return null;
    }

    private boolean readLine(
            final SessionInputBuffer buf, final boolean endOfStream) throws SMTPProtocolException {
        try {
            boolean lineComplete = buf.readLine(this.lineBuf, endOfStream);
            if (this.maxLineLen > 0 &&
                    (this.lineBuf.length() > this.maxLineLen ||
                            (!lineComplete && buf.length() > this.maxLineLen))) {
                throw new SMTPProtocolException("Maximum reply length limit exceeded");
            }
            return lineComplete;
        } catch (CharacterCodingException ex) {
            throw new SMTPProtocolException("Invalid character coding", ex);
        }
    }

    private ParsedLine parseLine() throws SMTPProtocolException {
        ParserCursor cursor = new ParserCursor(0, this.lineBuf.length());
        int code = parseCode(cursor);

        int codeClass = code / 100;
        if (codeClass <= 0) {
            throw new SMTPProtocolException("Malformed SMTP reply (invalid code): "
                    + this.lineBuf.toString());
        }
        boolean terminal = parseCodeDelimiter(cursor);
        SMTPCode enhancedCode;
        if (this.useEnhancedCodes && (codeClass == 2 || codeClass == 4 || codeClass == 5)) {
            enhancedCode = parseEnchancedCode(cursor);
            if (enhancedCode.getCodeClass() != codeClass) {
                throw new SMTPProtocolException("Malformed SMTP reply (code class mismatch): "
                        + this.lineBuf.toString());
            }
        } else {
            enhancedCode = null;
        }
        String text = parseText(cursor);
        this.lineBuf.clear();
        return new ParsedLine(code, terminal, enhancedCode, text);
    }

    private int parseCode(final ParserCursor cursor) throws SMTPProtocolException {
        int c;

        int i = cursor.getPos();
        while (i < cursor.getUpperBound()) {
            if (this.lineBuf.charAt(i) != ' ') {
                break;
            }
            i++;
        }

        if (cursor.getUpperBound() - i < 4) {
            throw new SMTPProtocolException("Malformed SMTP reply (no code): "
                    + this.lineBuf.toString());
        }
        try {
            c = Integer.parseInt(this.lineBuf.substring(i, i + 3));
        } catch (NumberFormatException ex) {
            throw new SMTPInvalidCodeException(this.lineBuf.toString());
        }
        cursor.updatePos(i + 3);
        return c;
    }

    private boolean parseCodeDelimiter(final ParserCursor cursor) throws SMTPProtocolException {
        boolean terminal;

        int i = cursor.getPos();
        int ch = this.lineBuf.charAt(i);
        if (ch == ' ') {
            terminal = true;
        } else if (ch == '-') {
            terminal = false;
        } else {
            throw new SMTPProtocolException("Malformed SMTP reply (invalid code separator): "
                    + this.lineBuf.toString());
        }
        cursor.updatePos(i + 1);
        return terminal;
    }

    private SMTPCode parseEnchancedCode(final ParserCursor cursor) throws SMTPProtocolException {
        int codeClass;
        int subject;
        int detail;

        int i1 = cursor.getPos();

        int i2 = this.lineBuf.indexOf('.', i1, cursor.getUpperBound());
        if (i2 == -1) {
            throw new SMTPInvalidCodeException(this.lineBuf.toString());
        }
        try {
            codeClass = Integer.parseInt(this.lineBuf.substring(i1, i2));
        } catch (NumberFormatException ex) {
            throw new SMTPInvalidCodeException(this.lineBuf.toString());
        }
        i1 = i2 + 1;
        i2 = this.lineBuf.indexOf('.', i1, cursor.getUpperBound());
        if (i2 == -1) {
            throw new SMTPInvalidCodeException(this.lineBuf.toString());
        }
        try {
            subject = Integer.parseInt(this.lineBuf.substring(i1, i2));
        } catch (NumberFormatException ex) {
            throw new SMTPInvalidCodeException(this.lineBuf.toString());
        }
        i1 = i2 + 1;
        i2 = this.lineBuf.indexOf(' ', i1, cursor.getUpperBound());
        if (i2 == -1) {
            throw new SMTPInvalidCodeException(this.lineBuf.toString());
        }
        try {
            detail = Integer.parseInt(this.lineBuf.substring(i1, i2));
        } catch (NumberFormatException ex) {
            throw new SMTPInvalidCodeException(this.lineBuf.toString());
        }
        cursor.updatePos(i2 + 1);
        return new SMTPCode(codeClass, subject, detail);
    }

    private String parseText(final ParserCursor cursor) throws SMTPProtocolException {
        String text = this.lineBuf.substringTrimmed(cursor.getPos(), cursor.getUpperBound());
        cursor.updatePos(cursor.getUpperBound());
        return text;
    }

    private static class ParsedLine {

        private final int code;
        private final boolean terminal;
        private final SMTPCode enhancedCode;
        private final String text;

        ParsedLine(final int code, final boolean terminal, final SMTPCode enhancedCode, final String text) {
            super();
            this.code = code;
            this.terminal = terminal;
            this.enhancedCode = enhancedCode;
            this.text = text;
        }

        public int getCode() {
            return code;
        }

        public boolean isTerminal() {
            return terminal;
        }

        public SMTPCode getEnhancedCode() {
            return enhancedCode;
        }

        public String getText() {
            return text;
        }

    }

    private static boolean sameCode(final ParsedLine l1, final ParsedLine l2) {
        int c1 = l1.getCode();
        int c2 = l2.getCode();
        SMTPCode e1 = l1.getEnhancedCode();
        SMTPCode e2 = l2.getEnhancedCode();
        return c1 == c2 && (e1 == null ? e2 == null : e1.equals(e2));
    }

}
