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
package com.ok2c.lightmtp.examples;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.util.concurrent.Future;

import com.ok2c.lightmtp.SMTPCode;
import com.ok2c.lightmtp.SMTPCodes;
import com.ok2c.lightmtp.SMTPConsts;
import com.ok2c.lightmtp.SMTPReply;
import com.ok2c.lightmtp.agent.MailServerTransport;
import com.ok2c.lightmtp.impl.agent.LocalMailServerTransport;
import com.ok2c.lightmtp.impl.protocol.BasicIdGenerator;
import com.ok2c.lightmtp.message.SMTPContent;
import com.ok2c.lightmtp.protocol.BasicDeliveryResult;
import com.ok2c.lightmtp.protocol.DeliveryHandler;
import com.ok2c.lightmtp.protocol.DeliveryRequest;
import com.ok2c.lightmtp.protocol.DeliveryResult;
import com.ok2c.lightmtp.protocol.EnvelopValidator;
import com.ok2c.lightmtp.protocol.RemoteAddressValidator;
import com.ok2c.lightmtp.util.InetAddressRange;
import com.ok2c.lightnio.concurrent.BasicFuture;
import com.ok2c.lightnio.concurrent.FutureCallback;
import com.ok2c.lightnio.impl.IOReactorConfig;

public class LocalMailServerTransportExample {

    public static void main(String[] args) throws Exception {
        final File workingDir = new File(".");
        final IOReactorConfig config = new IOReactorConfig();
        config.setWorkerCount(1);

        final MailServerTransport mta = new LocalMailServerTransport(workingDir, config);

        final InetSocketAddress sockaddress = new InetSocketAddress("localhost", 2525);

        InetAddressRange iprange = new InetAddressRange(InetAddress.getByName("127.0.0.0"), 8);

        mta.start(
                new BasicIdGenerator(),
                new MyRemoteAddressValidator(iprange),
                new MyEnvelopValidator(),
                new MyDeliveryHandler());
        mta.listen(sockaddress);

        System.out.println("LMTP server listening on "  + sockaddress);

        Runtime runtime = Runtime.getRuntime();
        runtime.addShutdownHook(new Thread() {

            @Override
            public void run() {
                try {
                    mta.shutdown();
                } catch (IOException ex) {
                    mta.forceShutdown();
                }
            }

        });
    }

    static class MyRemoteAddressValidator implements RemoteAddressValidator {

        private final InetAddressRange iprange;

        public MyRemoteAddressValidator(final InetAddressRange iprange) {
            super();
            this.iprange = iprange;
        }

        public boolean validateAddress(final InetAddress address) {
            return this.iprange.contains(address);
        }

    }

    static class MyEnvelopValidator implements EnvelopValidator {

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

    static class MyDeliveryHandler implements DeliveryHandler {

        public Future<DeliveryResult> handle(
                final String messageId,
                final DeliveryRequest request,
                final FutureCallback<DeliveryResult> callback) {
            BasicFuture<DeliveryResult> future = new BasicFuture<DeliveryResult>(callback);

            try {
                System.out.println("=====================================");
                System.out.println("Mail from " + request.getSender() + " to " + request.getRecipients());
                System.out.println("=====================================");
                System.out.println(readAsString(request.getContent()));
                System.out.println("=====================================");
                SMTPReply reply = new SMTPReply(SMTPCodes.OK, new SMTPCode(2, 1, 0), "ok");
                BasicDeliveryResult result = new BasicDeliveryResult(reply);
                future.completed(result);
            } catch (IOException ex) {
                future.failed(ex);
            }
            return future;
        }

        private String readAsString(
                final SMTPContent<ReadableByteChannel> content) throws IOException {
            StringBuilder buffer = new StringBuilder();
            ReadableByteChannel channel = content.channel();
            try {
                CharsetDecoder decoder = SMTPConsts.ASCII.newDecoder();
                ByteBuffer dst = ByteBuffer.allocate(1024);
                CharBuffer chars = CharBuffer.allocate(1024);
                int len;
                while ((len = channel.read(dst)) != -1) {
                    dst.flip();
                    CoderResult result = decoder.decode(dst, chars, len == -1);
                    if (result.isError()) {
                        result.throwException();
                    }
                    dst.compact();
                    chars.flip();
                    buffer.append(chars);
                    chars.compact();
                }
            } finally {
                content.reset();
            }
            return buffer.toString();
        }

    }

}
