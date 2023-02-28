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

import java.util.Map;

import jakarta.inject.Named;
import jakarta.ws.rs.Produces;
import org.apache.camel.component.infinispan.remote.InfinispanRemoteConfiguration;
import org.apache.camel.quarkus.component.infinispan.common.InfinispanCommonRoutes;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.marshall.ProtoStreamMarshaller;
import org.infinispan.commons.util.OS;

import static org.infinispan.client.hotrod.impl.ConfigurationProperties.CLIENT_INTELLIGENCE;
import static org.infinispan.client.hotrod.impl.ConfigurationProperties.MARSHALLER;

public class InfinispanRoutes extends InfinispanCommonRoutes {

    @Produces
    @Named("additionalConfig")
    Map<String, String> additionalInfinispanConfig() {
        Map<String, String> config = Map.of(MARSHALLER, ProtoStreamMarshaller.class.getName());
        if (OS.getCurrentOs().equals(OS.MAC_OS) || OS.getCurrentOs().equals(OS.WINDOWS)) {
            config.put(CLIENT_INTELLIGENCE, "BASIC");
        }
        return config;
    }

    @Override
    protected Configuration getConfigurationBuilder() {
        Config config = ConfigProvider.getConfig();
        ConfigurationBuilder clientBuilder = new ConfigurationBuilder();
        String[] hostParts = config.getValue("camel.component.infinispan.hosts", String.class).split(":");

        clientBuilder.addServer()
                .host(hostParts[0])
                .port(Integer.parseInt(hostParts[1]));

        clientBuilder
                .security()
                .authentication()
                .username(config.getValue("camel.component.infinispan.username", String.class))
                .password(config.getValue("camel.component.infinispan.password", String.class))
                .serverName(config.getValue("camel.component.infinispan.security-server-name", String.class))
                .saslMechanism(config.getValue("camel.component.infinispan.sasl-mechanism", String.class))
                .realm(config.getValue("camel.component.infinispan.security-realm", String.class))
                .marshaller(new ProtoStreamMarshaller());

        return clientBuilder.build();
    }

    @Override
    protected InfinispanRemoteConfiguration getConfiguration() {
        InfinispanRemoteConfiguration configuration = new InfinispanRemoteConfiguration();
        configuration.setCacheContainerConfiguration(getConfigurationBuilder());
        return configuration;
    }
}
