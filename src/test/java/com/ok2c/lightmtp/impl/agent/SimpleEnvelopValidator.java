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
package com.ok2c.lightmtp.impl.agent;

import java.net.InetAddress;
import java.util.concurrent.Future;

import org.apache.http.concurrent.BasicFuture;
import org.apache.http.concurrent.FutureCallback;

import com.ok2c.lightmtp.SMTPCode;
import com.ok2c.lightmtp.SMTPCodes;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.protocol.EnvelopValidator;

public class SimpleEnvelopValidator implements EnvelopValidator {

    @Override
    public Future<SMTPReply> validateRecipient(
            final InetAddress client,
            final String recipient,
            final FutureCallback<SMTPReply> callback) {
        BasicFuture<SMTPReply> future = new BasicFuture<SMTPReply>(callback);
        SMTPReply reply = new SMTPReply(SMTPCodes.OK, new SMTPCode(2, 1, 5),
                "recipient <" + recipient + "> ok");
        future.completed(reply);
        return future;
    }

    @Override
    public Future<SMTPReply> validateSender(
            final InetAddress client,
            final String sender,
            final FutureCallback<SMTPReply> callback) {
        BasicFuture<SMTPReply> future = new BasicFuture<SMTPReply>(callback);
        SMTPReply reply = new SMTPReply(SMTPCodes.OK, new SMTPCode(2, 1, 0),
                "originator <" + sender + "> ok");
        future.completed(reply);
        return future;
    }

}
