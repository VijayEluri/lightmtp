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
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.Future;

import com.ok2c.lightmtp.agent.MailUserAgent;
import com.ok2c.lightmtp.agent.TransportType;
import com.ok2c.lightmtp.impl.agent.DefaultMailUserAgent;
import com.ok2c.lightmtp.message.content.FileSource;
import com.ok2c.lightmtp.protocol.BasicDeliveryRequest;
import com.ok2c.lightmtp.protocol.DeliveryRequest;
import com.ok2c.lightmtp.protocol.DeliveryResult;
import com.ok2c.lightnio.impl.IOReactorConfig;

public class SendMailExample {

    public static void main(String[] args) throws Exception {

        if (args.length < 3) {
            System.out.println("Usage: sender recipient1[;recipient2;recipient3;...] file");
            System.exit(0);
        }

        String sender = args[0];
        List<String> recipients = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer(args[1], ";");
        while (tokenizer.hasMoreTokens()) {
            String s = tokenizer.nextToken();
            s = s.trim();
            if (s.length() > 0) {
                recipients.add(s);
            }
        }

        File src = new File(args[2]);
        if (!src.exists()) {
            System.out.println("File '" + src + "' does not exist");
            System.exit(0);
        }

        DeliveryRequest request = new BasicDeliveryRequest(sender, recipients, new FileSource(src));

        MailUserAgent mua = new DefaultMailUserAgent(TransportType.SMTP, new IOReactorConfig());
        mua.start();

        try {

            InetSocketAddress address = new InetSocketAddress("localhost", 2525);

            Future<DeliveryResult> future = mua.deliver(address, request, null);

            DeliveryResult result = future.get();
            System.out.println("Delivery result: " + result);

        } finally {
            mua.shutdown();
        }
    }

}
