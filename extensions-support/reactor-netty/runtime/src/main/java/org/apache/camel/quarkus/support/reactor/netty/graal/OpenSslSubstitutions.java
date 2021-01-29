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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.RecomputeFieldValue.Kind;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import io.netty.handler.ssl.OpenSsl;

/**
 * JDK is the only supported SSL provider on Quarkus. We thus substitute the methods of {@link OpenSsl} to the effect
 * that OpenSSL is not available.
 */
//TODO: move this to quarkus-netty https://github.com/apache/camel-quarkus/issues/2142
@TargetClass(OpenSsl.class)
final class OpenSslSubstitutions {

    @Alias
    @RecomputeFieldValue(kind = Kind.FromAlias)
    private static Throwable UNAVAILABILITY_CAUSE = new RuntimeException("OpenSsl unsupported on Quarkus");

    @Alias
    @RecomputeFieldValue(kind = Kind.FromAlias)
    static List<String> DEFAULT_CIPHERS = Collections.emptyList();

    @Alias
    @RecomputeFieldValue(kind = Kind.FromAlias)
    static Set<String> AVAILABLE_CIPHER_SUITES = Collections.emptySet();

    @Alias
    @RecomputeFieldValue(kind = Kind.FromAlias)
    private static Set<String> AVAILABLE_OPENSSL_CIPHER_SUITES = Collections.emptySet();

    @Alias
    @RecomputeFieldValue(kind = Kind.FromAlias)
    private static Set<String> AVAILABLE_JAVA_CIPHER_SUITES = Collections.emptySet();

    @Alias
    @RecomputeFieldValue(kind = Kind.FromAlias)
    private static boolean SUPPORTS_KEYMANAGER_FACTORY = false;

    @Alias
    @RecomputeFieldValue(kind = Kind.FromAlias)
    private static boolean SUPPORTS_OCSP = false;

    @Alias
    @RecomputeFieldValue(kind = Kind.FromAlias)
    static Set<String> SUPPORTED_PROTOCOLS_SET = Collections.emptySet();

    @Substitute
    public static boolean isAvailable() {
        return false;
    }

    @Substitute
    public static int version() {
        return -1;
    }

    @Substitute
    public static String versionString() {
        return null;
    }

    @Substitute
    public static boolean isCipherSuiteAvailable(String cipherSuite) {
        return false;
    }

}
