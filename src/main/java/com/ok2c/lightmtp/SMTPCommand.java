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

public class SMTPCommand {

    private final String verb;
    private final String argument;
    private final List<String> params;

    public SMTPCommand(final String code, final String argument, final List<String> params) {
        super();
        if (code == null) {
            throw new IllegalArgumentException("Code may not be null");
        }
        this.verb = code;
        this.argument = argument;
        if (params == null || params.isEmpty()) {
            this.params = Collections.emptyList();
        } else {
            this.params = Collections.unmodifiableList(new ArrayList<String>(params));
        }
    }

    public SMTPCommand(final String code, final String argument) {
        this(code, argument, null);
    }

    public SMTPCommand(final String code) {
        this(code, null, null);
    }

    public String getVerb() {
        return this.verb;
    }

    public String getArgument() {
        return this.argument;
    }

    public List<String> getParams() {
        return params;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(this.verb);
        buffer.append(' ');
        buffer.append(this.argument);
        for (String param: this.params) {
            buffer.append(' ');
            buffer.append(param);
        }
        return buffer.toString();
    }

}
