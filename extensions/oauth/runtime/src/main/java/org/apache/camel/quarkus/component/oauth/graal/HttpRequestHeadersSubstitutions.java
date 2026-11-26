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
package org.apache.camel.quarkus.component.oauth.graal;

import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.camel.Message;

/**
 * Removes references to the optional camel-http-common and camel-netty-http from camel-oauth. Their request header
 * readers are only called for a message of their own transport, so they are never reached when it is absent.
 */
final class HttpRequestHeadersSubstitutions {
    static final String SERVLET_HTTP_MESSAGE = "org.apache.camel.http.common.HttpMessage";
    static final String NETTY_HTTP_MESSAGE = "org.apache.camel.component.netty.http.NettyHttpMessage";

    static boolean isAbsent(String className) {
        try {
            Thread.currentThread().getContextClassLoader().loadClass(className);
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }

    static final class CamelHttpCommonAbsent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return isAbsent(SERVLET_HTTP_MESSAGE);
        }
    }

    static final class CamelNettyHttpAbsent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return isAbsent(NETTY_HTTP_MESSAGE);
        }
    }
}

@TargetClass(className = "org.apache.camel.oauth.ServletHttpRequestHeaders", onlyWith = HttpRequestHeadersSubstitutions.CamelHttpCommonAbsent.class)
final class Target_org_apache_camel_oauth_ServletHttpRequestHeaders {
    @Substitute
    static Optional<List<String>> values(Message message, String name) {
        return Optional.empty();
    }

    @Substitute
    static Optional<List<String>> valuesOfSpringBootMessage(Message message, String name) {
        return Optional.empty();
    }
}

@TargetClass(className = "org.apache.camel.oauth.NettyHttpRequestHeaders", onlyWith = HttpRequestHeadersSubstitutions.CamelNettyHttpAbsent.class)
final class Target_org_apache_camel_oauth_NettyHttpRequestHeaders {
    @Substitute
    static Optional<List<String>> values(Message message, String name) {
        return Optional.empty();
    }
}
