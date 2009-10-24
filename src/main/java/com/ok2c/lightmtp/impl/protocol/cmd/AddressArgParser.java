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
package com.ok2c.lightmtp.impl.protocol.cmd;

import com.ok2c.lightmtp.SMTPProtocolException;

class AddressArgParser {

    private final String prefix;

    public AddressArgParser(final String prefix) {
        super();
        this.prefix = prefix;
    }

    String parse(final String argument) throws SMTPProtocolException {
        if (argument == null) {
            throw new SMTPProtocolException("Argument missing");
        }
        if (argument.length() < this.prefix.length() + 2) {
            throw new SMTPProtocolException("Invalid argument: " + argument);
        }
        String s = argument.substring(0, this.prefix.length());
        if (!s.equalsIgnoreCase(this.prefix)) {
            throw new SMTPProtocolException("Invalid argument: " + argument);
        }
        String sender = argument.substring(this.prefix.length()).trim();
        if (sender.startsWith("<") && sender.endsWith(">")) {
            return sender.substring(1, sender.length() - 1);
        } else {
            throw new SMTPProtocolException("Invalid argument: " + argument);
        }
    }

}
