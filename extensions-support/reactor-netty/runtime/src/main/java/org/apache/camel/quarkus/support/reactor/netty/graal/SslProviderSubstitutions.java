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
package org.apache.camel.quarkus.support.reactor.netty.graal;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import io.netty.handler.ssl.IdentityCipherSuiteFilter;
import io.netty.handler.ssl.SslContextBuilder;
import reactor.netty.tcp.SslProvider;

@TargetClass(SslProvider.class)
//TODO: move this to quarkus-netty https://github.com/apache/camel-quarkus/issues/2142
public final class SslProviderSubstitutions {

    @Alias
    SslContextBuilder sslContextBuilder;

    @Substitute
    void updateDefaultConfiguration() {
        /* We hardcode the reactor.netty.tcp.SslProvider.DefaultConfigurationType.TCP path from the original method
         * to avoid requiring io.netty.handler.ssl.OpenSsl */
        sslContextBuilder.sslProvider(io.netty.handler.ssl.SslProvider.JDK)
                .ciphers(null, IdentityCipherSuiteFilter.INSTANCE)
                .applicationProtocolConfig(null);
    }

}
