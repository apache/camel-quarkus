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

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.camel.Message;

/**
 * camel-oauth reads HTTP request headers through a helper class per HTTP transport. A helper is only invoked for a
 * message of its own transport, so when that transport is not on the classpath, its helper is replaced so that the
 * native image can link it.
 * <p>
 * Whether a transport is present is decided by the message type that camel-oauth itself dispatches on, read from the
 * constants in {@code HttpRequestHeaders}. If those constants change, the native build fails. The replacement helpers
 * throw rather than return no headers, so that a wrongly substituted helper fails the request instead of falling back
 * to
 * a less strict way of reading credentials.
 */
final class HttpRequestHeadersSubstitutions {

    private static final String HTTP_REQUEST_HEADERS = "org.apache.camel.oauth.HttpRequestHeaders";

    static boolean isDispatchTypeAbsent(String constantName) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String className;
        try {
            Field field = Class.forName(HTTP_REQUEST_HEADERS, false, classLoader).getDeclaredField(constantName);
            field.setAccessible(true);
            className = (String) field.get(null);
        } catch (ReflectiveOperationException | ClassCastException e) {
            throw new IllegalStateException("Unable to read " + HTTP_REQUEST_HEADERS + "." + constantName
                    + ". The camel-oauth native substitutions in " + HttpRequestHeadersSubstitutions.class.getName()
                    + " need to be updated", e);
        }

        try {
            classLoader.loadClass(className);
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }

    static IllegalStateException transportAbsent(String transport) {
        return new IllegalStateException("Unable to read the HTTP request headers of a " + transport + " message."
                + " The " + transport + " request header support was removed from the native image because the "
                + transport + " message type was not detected at build time");
    }

    static final class ServletIsAbsent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return isDispatchTypeAbsent("SERVLET_HTTP_MESSAGE");
        }
    }

    static final class NettyHttpIsAbsent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return isDispatchTypeAbsent("NETTY_HTTP_MESSAGE");
        }
    }

    static final class PlatformHttpVertxIsAbsent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return isDispatchTypeAbsent("VERTX_HTTP_MESSAGE");
        }
    }
}

@TargetClass(className = "org.apache.camel.oauth.ServletHttpRequestHeaders", onlyWith = HttpRequestHeadersSubstitutions.ServletIsAbsent.class)
final class ServletHttpRequestHeadersSubstitutions {

    @Substitute
    static Optional<List<String>> values(Message message, String name) {
        throw HttpRequestHeadersSubstitutions.transportAbsent("servlet");
    }

    @Substitute
    static Optional<List<String>> valuesOfSpringBootMessage(Message message, String name) {
        throw HttpRequestHeadersSubstitutions.transportAbsent("servlet");
    }
}

@TargetClass(className = "org.apache.camel.oauth.NettyHttpRequestHeaders", onlyWith = HttpRequestHeadersSubstitutions.NettyHttpIsAbsent.class)
final class NettyHttpRequestHeadersSubstitutions {

    @Substitute
    static Optional<List<String>> values(Message message, String name) {
        throw HttpRequestHeadersSubstitutions.transportAbsent("netty-http");
    }
}

@TargetClass(className = "org.apache.camel.oauth.VertxHttpRequestHeaders", onlyWith = HttpRequestHeadersSubstitutions.PlatformHttpVertxIsAbsent.class)
final class VertxHttpRequestHeadersSubstitutions {

    @Substitute
    static Optional<List<String>> values(Message message, String name) {
        throw HttpRequestHeadersSubstitutions.transportAbsent("platform-http");
    }
}
