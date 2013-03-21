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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.ok2c.lightmtp.protocol.ProtocolCodec;
import com.ok2c.lightmtp.protocol.ProtocolCodecs;

public class ProtocolCodecRegistry<T> implements ProtocolCodecs<T> {

    private final Map<String, ProtocolCodec<T>> codecs;

    public ProtocolCodecRegistry() {
        super();
        this.codecs = new HashMap<String, ProtocolCodec<T>>();
    }

    @Override
    public void register(final String name, final ProtocolCodec<T> codec) {
        this.codecs.put(name, codec);
    }

    @Override
    public void unregister(final String name) {
        this.codecs.remove(name);
    }

    @Override
    public Set<String> getCodecNames() {
        return this.codecs.keySet();
    }

    @Override
    public ProtocolCodec<T> getCodec(final String name) {
        ProtocolCodec<T> codec = this.codecs.get(name);
        if (codec != null) {
            return codec;
        } else {
            throw new IllegalStateException(name + " is not available");
        }
    }

}
