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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SMTPReply {

    private final int code;
    private final SMTPCode enhancedCode;
    private final List<String> lines;

    public SMTPReply(int code, final SMTPCode enhancedCode, final List<String> lines) {
        super();
        if (code <= 0) {
            throw new IllegalArgumentException("Code may not be nagtive or zero");
        }
        this.code = code;
        this.enhancedCode = enhancedCode;
        if (lines == null || lines.isEmpty()) {
            this.lines = Collections.emptyList();
        } else {
            this.lines = Collections.unmodifiableList(new ArrayList<String>(lines));
        }
    }

    public SMTPReply(int code, final SMTPCode enhancedCode, final String line) {
        super();
        if (code <= 0) {
            throw new IllegalArgumentException("Code may not be nagtive or zero");
        }
        if (line == null) {
            throw new IllegalArgumentException("Line may not be null");
        }
        this.code = code;
        this.enhancedCode = enhancedCode;
        List<String> lines = new ArrayList<String>();
        lines.add(line);
        this.lines = Collections.unmodifiableList(new ArrayList<String>(lines));
    }

    public int getCode() {
        return this.code;
    }

    public SMTPCode getEnhancedCode() {
        return this.enhancedCode;
    }

    public List<String> getLines() {
        return this.lines;
    }

    public String getLine() {
        if (this.lines.size() == 1) {
            return this.lines.get(0);
        } else {
            return this.lines.toString();
        }
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(this.code);
        buffer.append(' ');
        if (this.enhancedCode != null) {
            buffer.append(this.enhancedCode);
            buffer.append(' ');
        }
        buffer.append(getLine());
        return buffer.toString();
    }

}
