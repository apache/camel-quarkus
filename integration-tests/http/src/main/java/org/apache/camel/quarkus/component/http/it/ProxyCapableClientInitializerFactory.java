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

import java.net.InetSocketAddress;

import io.netty.channel.Channel;
import io.netty.handler.proxy.HttpProxyHandler;
import org.apache.camel.component.netty.ClientInitializerFactory;
import org.apache.camel.component.netty.NettyProducer;
import org.apache.camel.component.netty.http.HttpClientInitializerFactory;
import org.apache.camel.component.netty.http.NettyHttpProducer;
import org.eclipse.microprofile.config.ConfigProvider;

import static org.apache.camel.quarkus.component.http.it.HttpResource.USER_ADMIN;
import static org.apache.camel.quarkus.component.http.it.HttpResource.USER_ADMIN_PASSWORD;

public class ProxyCapableClientInitializerFactory extends HttpClientInitializerFactory {

    public ProxyCapableClientInitializerFactory() {
    }

    public ProxyCapableClientInitializerFactory(NettyHttpProducer producer) {
        super(producer);
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        Integer proxyPort = ConfigProvider.getConfig().getValue("tiny.proxy.port", Integer.class);
        InetSocketAddress proxyServerAddress = new InetSocketAddress("localhost", proxyPort);
        HttpProxyHandler httpProxyHandler = new HttpProxyHandler(proxyServerAddress, USER_ADMIN, USER_ADMIN_PASSWORD);
        httpProxyHandler.setConnectTimeoutMillis(5000);
        super.initChannel(channel);
        channel.pipeline().addFirst(httpProxyHandler);
    }

    @Override
    public ClientInitializerFactory createPipelineFactory(NettyProducer producer) {
        return new ProxyCapableClientInitializerFactory((NettyHttpProducer) producer);
    }
}
