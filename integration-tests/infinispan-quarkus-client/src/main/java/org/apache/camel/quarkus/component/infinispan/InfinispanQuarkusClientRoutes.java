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

import java.util.Set;

import org.apache.camel.CamelContext;
import org.apache.camel.component.infinispan.remote.InfinispanRemoteComponent;
import org.apache.camel.component.infinispan.remote.InfinispanRemoteConfiguration;
import org.apache.camel.quarkus.component.infinispan.common.InfinispanCommonRoutes;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.marshall.ProtoStreamMarshaller;

public class InfinispanQuarkusClientRoutes extends InfinispanCommonRoutes {

    @Override
    protected Configuration getConfigurationBuilder() {
        Config config = ConfigProvider.getConfig();
        ConfigurationBuilder clientBuilder = new ConfigurationBuilder();
        String[] hostParts = config.getValue("quarkus.infinispan-client.server-list", String.class).split(":");

        clientBuilder.addServer()
                .host(hostParts[0])
                .port(Integer.parseInt(hostParts[1]));

        clientBuilder
                .security()
                .authentication()
                .username(config.getValue("quarkus.infinispan-client.auth-username", String.class))
                .password(config.getValue("quarkus.infinispan-client.auth-password", String.class))
                .serverName(config.getValue("quarkus.infinispan-client.auth-server-name", String.class))
                .saslMechanism(config.getValue("quarkus.infinispan-client.sasl-mechanism", String.class))
                .realm(config.getValue("quarkus.infinispan-client.auth-realm", String.class))
                .marshaller(new ProtoStreamMarshaller());

        return clientBuilder.build();
    }

    @Override
    protected InfinispanRemoteConfiguration getConfiguration() {
        CamelContext camelContext = getCamelContext();
        InfinispanRemoteComponent component = camelContext.getComponent("infinispan", InfinispanRemoteComponent.class);
        InfinispanRemoteConfiguration configuration = component.getConfiguration().clone();
        configuration.setCacheContainerConfiguration(getConfigurationBuilder());
        Set<RemoteCacheManager> beans = camelContext.getRegistry().findByType(RemoteCacheManager.class);
        RemoteCacheManager cacheManager = beans.iterator().next();
        configuration.setCacheContainer(cacheManager);
        return configuration;
    }
}
