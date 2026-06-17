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
package org.apache.camel.quarkus.component.github2.graal;

import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.kohsuke.github.HttpConnector;
import org.kohsuke.github.connector.GitHubConnector;
import org.kohsuke.github.extras.HttpClientGitHubConnector;
import org.kohsuke.github.internal.DefaultGitHubConnector;
import org.kohsuke.github.internal.GitHubConnectorHttpConnectorAdapter;

@TargetClass(value = DefaultGitHubConnector.class, onlyWith = DefaultGitHubConnectorSubstitutions.OkHttpIsAbsent.class)
final class DefaultGitHubConnectorSubstitutions {

    @Substitute
    static GitHubConnector create(String connectorSpec) {
        if ("httpclient".equalsIgnoreCase(connectorSpec)) {
            return new HttpClientGitHubConnector();
        } else if ("default".equalsIgnoreCase(connectorSpec)) {
            try {
                return new HttpClientGitHubConnector();
            } catch (UnsupportedOperationException | LinkageError e) {
                return new GitHubConnectorHttpConnectorAdapter(HttpConnector.DEFAULT);
            }
        } else if ("urlconnection".equalsIgnoreCase(connectorSpec)) {
            return new GitHubConnectorHttpConnectorAdapter(HttpConnector.DEFAULT);
        }
        throw new IllegalStateException(
                "Property 'test.github.connector' must reference a valid built-in connector - httpclient, urlconnection, or default."
                        + " (okhttp and okhttpconnector require the OkHttp library on the classpath.)");
    }

    static final class OkHttpIsAbsent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            try {
                Thread.currentThread().getContextClassLoader().loadClass("okhttp3.OkHttpClient");
                return false;
            } catch (ClassNotFoundException e) {
                return true;
            }
        }
    }
}
