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
package org.apache.camel.quarkus.component.http.it;

import javax.inject.Named;

import org.apache.camel.component.netty.ClientInitializerFactory;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.protocol.HttpContext;
import org.eclipse.microprofile.config.ConfigProvider;

import static org.apache.camel.quarkus.component.http.it.HttpResource.USER_ADMIN;
import static org.apache.camel.quarkus.component.http.it.HttpResource.USER_ADMIN_PASSWORD;

public class HttpProducers {

    @Named
    public ClientInitializerFactory proxyCapableClientInitializerFactory() {
        return new ProxyCapableClientInitializerFactory();
    }

    @Named
    HttpContext basicAuthContext() {
        Integer port = ConfigProvider.getConfig().getValue("quarkus.http.test-port", Integer.class);

        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(USER_ADMIN, USER_ADMIN_PASSWORD);
        CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(AuthScope.ANY, credentials);

        BasicAuthCache authCache = new BasicAuthCache();
        BasicScheme basicAuth = new BasicScheme();
        authCache.put(new HttpHost("localhost", port), basicAuth);

        HttpClientContext context = HttpClientContext.create();
        context.setAuthCache(authCache);
        context.setCredentialsProvider(provider);

        return context;
    }
}
