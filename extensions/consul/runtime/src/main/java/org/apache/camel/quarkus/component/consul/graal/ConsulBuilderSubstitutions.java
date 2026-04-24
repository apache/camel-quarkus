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
package org.apache.camel.quarkus.component.consul.graal;

import java.nio.file.Path;
import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import okhttp3.OkHttpClient;
import org.kiwiproject.consul.Consul;

@TargetClass(value = Consul.Builder.class, onlyWith = { ConsulBuilderSubstitutions.JUnixSocketIsAbsent.class })
final class ConsulBuilderSubstitutions {
    @Substitute
    static void addUnixDomainSocketFactory(Path unixSocketPath, OkHttpClient.Builder builder) {
        // NoOp since optional dependency com.kohlschutter.junixsocket:junixsocket-core is not on the classpath
    }

    static final class JUnixSocketIsAbsent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            try {
                Thread.currentThread().getContextClassLoader().loadClass("org.newsclub.net.unix.AFUNIXSocket");
                return false;
            } catch (ClassNotFoundException e) {
                return true;
            }
        }
    }
}
