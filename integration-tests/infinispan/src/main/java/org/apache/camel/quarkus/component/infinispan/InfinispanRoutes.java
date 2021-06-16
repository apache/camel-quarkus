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
package org.apache.camel.quarkus.component.infinispan;

import java.nio.charset.StandardCharsets;

import javax.inject.Named;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.infinispan.InfinispanConstants;
import org.apache.camel.component.infinispan.InfinispanOperation;
import org.apache.camel.component.infinispan.remote.InfinispanRemoteComponent;
import org.apache.camel.component.infinispan.remote.InfinispanRemoteComponentConfigurer;

public class InfinispanRoutes extends RouteBuilder {
    @Override
    public void configure() {
        // we do not need to set any information about the target infinispan server
        // as the RemoteConnectionManager is produced by the infinispan extension
        // and camel-main automatically bind it to the component

        from("direct:put")
                .convertBodyTo(byte[].class)
                .to("log:cache?showAll=true")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.PUT)
                .setHeader(InfinispanConstants.KEY).constant("the-key".getBytes(StandardCharsets.UTF_8))
                .setHeader(InfinispanConstants.VALUE).body()
                .toD("${header.component}:${header.cacheName}")
                .to("log:put?showAll=true");

        from("direct:get")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.GET)
                .setHeader(InfinispanConstants.KEY).constant("the-key".getBytes(StandardCharsets.UTF_8))
                .toD("${header.component}:${header.cacheName}")
                .to("log:get?showAll=true");
    }

    @Named("infinispan-quarkus")
    public InfinispanRemoteComponent infinispanQuarkus() {
        // This component will have its cacheContainer option autowired to use the one created by the Quarkus Infinispan extension
        return new InfinispanRemoteComponent();
    }

    @Named("infinispan-quarkus-component")
    public InfinispanRemoteComponentConfigurer quarkusInfinispanConfigurer() {
        return new InfinispanRemoteComponentConfigurer();
    }
}
