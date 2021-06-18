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
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Realm;
import org.asynchttpclient.proxy.ProxyServer;
import org.asynchttpclient.proxy.ProxyType;
import org.eclipse.microprofile.config.ConfigProvider;

import static org.apache.camel.quarkus.component.http.it.HttpResource.USER_ADMIN;
import static org.apache.camel.quarkus.component.http.it.HttpResource.USER_ADMIN_PASSWORD;

public class HttpProducers {

    @Named
    public ClientInitializerFactory proxyCapableClientInitializerFactory() {
        return new ProxyCapableClientInitializerFactory();
    }

    @Named
    public AsyncHttpClient asyncHttpClientWithProxy() {
        Integer proxyPort = ConfigProvider.getConfig().getValue("tiny.proxy.port", Integer.class);

        Realm realm = new Realm.Builder(USER_ADMIN, USER_ADMIN_PASSWORD)
                .setScheme(Realm.AuthScheme.BASIC)
                .build();

        ProxyServer proxyServer = new ProxyServer.Builder("localhost", proxyPort)
                .setProxyType(ProxyType.HTTP)
                .setRealm(realm)
                .build();

        AsyncHttpClientConfig config = new DefaultAsyncHttpClientConfig.Builder()
                .setRealm(realm)
                .setProxyServer(proxyServer)
                .build();

        return new DefaultAsyncHttpClient(config);
    }
}
