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
package org.apache.camel.quarkus.support.google.cloud.graal;

import java.util.function.BooleanSupplier;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import static org.apache.camel.quarkus.support.google.cloud.graal.DefaultHttpTransportFactorySubstitutions.DefaultHttpTransportFactoryPresent;

@TargetClass(className = "com.google.cloud.http.HttpTransportOptions$DefaultHttpTransportFactory", onlyWith = DefaultHttpTransportFactoryPresent.class)
public final class DefaultHttpTransportFactorySubstitutions {

    @Substitute
    public HttpTransport create() {
        // Suppress creation of UrlFetchTransport for GAE which is not supported in native mode
        return new NetHttpTransport();
    }

    static final class DefaultHttpTransportFactoryPresent implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            try {
                Thread.currentThread().getContextClassLoader()
                        .loadClass("com.google.cloud.http.HttpTransportOptions$DefaultHttpTransportFactory");
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
    }
}
