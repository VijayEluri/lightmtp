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

import java.util.List;
import java.util.Locale;

import com.ok2c.lightmtp.SMTPCode;
import com.ok2c.lightmtp.SMTPCodes;
import com.ok2c.lightmtp.SMTPErrorException;
import com.ok2c.lightmtp.impl.protocol.MIMEEncoding;
import com.ok2c.lightmtp.impl.protocol.ServerState;
import com.ok2c.lightmtp.protocol.Action;
import com.ok2c.lightmtp.protocol.CommandHandler;
import com.ok2c.lightmtp.protocol.EnvelopValidator;

public class MailFromHandler implements CommandHandler<ServerState> {

    private final EnvelopValidator validator;
    private final AddressArgParser argParser;

    public MailFromHandler(final EnvelopValidator validator) {
        super();
        this.validator = validator;
        this.argParser = new AddressArgParser("FROM:");
    }

    public Action<ServerState> handle(
            final String argument,
            final List<String> params) throws SMTPErrorException {
        String sender = this.argParser.parse(argument);
        MIMEEncoding mimeEncoding = null;
        if (params == null || params.size() == 0) {
            mimeEncoding = MIMEEncoding.MIME_7BIT;
        } else if (params.size() == 1) {
            String s = params.get(0).toUpperCase(Locale.US);
            if (s.startsWith("BODY=")) {
                s = s.substring(5);
                if (s.equals("7BIT")) {
                    mimeEncoding = MIMEEncoding.MIME_7BIT;
                } else if (s.equals("8BITMIME")) {
                    mimeEncoding = MIMEEncoding.MIME_8BIT;
                }
            }
        }
        if (mimeEncoding == null) {
            throw new SMTPErrorException(
                    SMTPCodes.ERR_PERM_SYNTAX_ERR_PARAM,
                    new SMTPCode(5, 5, 4),
                    "invalid parameter(s): " + params);
        }
        return new MailFromAction(sender, mimeEncoding, this.validator);
    }

}
