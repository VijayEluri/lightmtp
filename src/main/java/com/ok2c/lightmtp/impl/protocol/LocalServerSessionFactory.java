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
package com.ok2c.lightmtp.impl.protocol;

import java.io.File;

import org.apache.http.nio.reactor.IOSession;
import org.apache.http.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ok2c.lightmtp.impl.protocol.cmd.DataHandler;
import com.ok2c.lightmtp.impl.protocol.cmd.DefaultProtocolHandler;
import com.ok2c.lightmtp.impl.protocol.cmd.EhloHandler;
import com.ok2c.lightmtp.impl.protocol.cmd.MailFromHandler;
import com.ok2c.lightmtp.impl.protocol.cmd.NoopHandler;
import com.ok2c.lightmtp.impl.protocol.cmd.QuitHandler;
import com.ok2c.lightmtp.impl.protocol.cmd.RcptToHandler;
import com.ok2c.lightmtp.impl.protocol.cmd.RsetHandler;
import com.ok2c.lightmtp.impl.protocol.cmd.VrfyHandler;
import com.ok2c.lightmtp.protocol.DeliveryHandler;
import com.ok2c.lightmtp.protocol.EnvelopValidator;
import com.ok2c.lightmtp.protocol.ProtocolCodecs;
import com.ok2c.lightmtp.protocol.ProtocolHandler;
import com.ok2c.lightmtp.protocol.RemoteAddressValidator;
import com.ok2c.lightmtp.protocol.SessionFactory;
import com.ok2c.lightmtp.protocol.UniqueIdGenerator;

public class LocalServerSessionFactory implements SessionFactory<ServerSession> {

    private final Logger iolog = LoggerFactory.getLogger(IOSession.class);
    private final Logger wirelog = LoggerFactory.getLogger(Wire.WIRELOG_CAT);

    private final File workingDir;
    private final UniqueIdGenerator idgenerator;
    private final RemoteAddressValidator addressValidator;
    private final EnvelopValidator validator;
    private final DeliveryHandler deliveryHandler;

    public LocalServerSessionFactory(
            final File workingDir,
            final UniqueIdGenerator idgenerator,
            final RemoteAddressValidator addressValidator,
            final EnvelopValidator validator,
            final DeliveryHandler deliveryHandler) {
        super();
        Args.notNull(workingDir, "Working dir");
        Args.notNull(idgenerator, "Id generator");
        Args.notNull(validator, "Envelop validator");
        Args.notNull(deliveryHandler, "Delivery handler");
        this.workingDir = workingDir;
        this.idgenerator = idgenerator;
        this.addressValidator = addressValidator;
        this.validator = validator;
        this.deliveryHandler = deliveryHandler;
    }

    @Override
    public ServerSession create(final IOSession iosession) {
        SMTPBuffers iobuffers = new SMTPBuffers();
        ProtocolCodecs<ServerState> codecs = new ProtocolCodecRegistry<ServerState>();
        codecs.register(ProtocolState.INIT.name(),
                new ServiceReadyCodec(iobuffers, this.addressValidator));
        codecs.register(ProtocolState.MAIL.name(),
                new PipeliningReceiveEnvelopCodec(iobuffers,
                        createProtocolHandler(this.idgenerator, this.validator)));
        codecs.register(ProtocolState.DATA.name(),
                new ReceiveDataCodec(iobuffers, this.workingDir, this.deliveryHandler,
                        DataAckMode.PER_RECIPIENT));
        codecs.register(ProtocolState.QUIT.name(),
                new ServiceShutdownCodec(iobuffers));
        final IOSession localIOSession;
        if (iolog.isDebugEnabled() || wirelog.isDebugEnabled()) {
            localIOSession = new LoggingIOSession(iosession, "LMTP server", iolog, wirelog);
        } else {
            localIOSession = iosession;
        }
        return new ServerSession(localIOSession, iobuffers, codecs);
    }

    protected ProtocolHandler<ServerState> createProtocolHandler(
            final UniqueIdGenerator idgenerator,
            final EnvelopValidator validator) {
        DefaultProtocolHandler handler = new DefaultProtocolHandler();
        handler.register("LHLO", new EhloHandler());
        handler.register("RSET", new RsetHandler());
        handler.register("NOOP", new NoopHandler());
        handler.register("QUIT", new QuitHandler());
        handler.register("VRFY", new VrfyHandler(validator));
        handler.register("MAIL", new MailFromHandler(idgenerator, validator));
        handler.register("RCPT", new RcptToHandler(validator));
        handler.register("DATA", new DataHandler());
        return handler;
    }

}
