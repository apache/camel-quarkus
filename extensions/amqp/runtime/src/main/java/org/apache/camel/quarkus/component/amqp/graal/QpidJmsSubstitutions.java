/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.quarkus.component.amqp.graal;

import java.net.URI;

import javax.net.ssl.SSLEngine;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.ssl.SslContext;
import org.apache.qpid.jms.transports.TransportOptions;

final class QpidJmsSubstitutions {
}

@TargetClass(className = "org.apache.qpid.jms.transports.TransportSupport")
final class SubstituteTransportSupport {

    @Substitute
    public static boolean isOpenSSLPossible(TransportOptions options) {
        return false;
    }

    @Substitute
    public static SSLEngine createOpenSslEngine(ByteBufAllocator allocator, URI remote, SslContext context,
            TransportOptions options) throws Exception {
        throw new IllegalStateException("OpenSSL support is disabled in native mode");
    }

    @Substitute
    public static SslContext createOpenSslContext(TransportOptions options) throws Exception {
        throw new IllegalStateException("OpenSSL support is disabled in native mode");
    }
}
