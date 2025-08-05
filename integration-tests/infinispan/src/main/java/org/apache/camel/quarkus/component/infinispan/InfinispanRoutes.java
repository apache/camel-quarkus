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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import org.apache.camel.component.infinispan.InfinispanComponent;
import org.apache.camel.component.infinispan.InfinispanConstants;
import org.apache.camel.component.infinispan.InfinispanOperation;
import org.apache.camel.component.infinispan.remote.InfinispanRemoteComponent;
import org.apache.camel.component.infinispan.remote.InfinispanRemoteConfiguration;
import org.apache.camel.quarkus.component.infinispan.common.InfinispanCommonRoutes;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.marshall.ProtoStreamMarshaller;
import org.infinispan.commons.util.OS;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.schema.Schema;
import org.infinispan.protostream.schema.Type;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;

import static org.infinispan.client.hotrod.impl.ConfigurationProperties.CLIENT_INTELLIGENCE;
import static org.infinispan.client.hotrod.impl.ConfigurationProperties.MARSHALLER;

public class InfinispanRoutes extends InfinispanCommonRoutes {

    @Override
    public void configure() {
        super.configure();

        from("direct:infinispan-query")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.QUERY)
                .toF("infinispan-query:%s", CACHE_NAME);
    }

    private final static Schema SCHEMA_PERSON = new Schema.Builder("person.proto")
            .packageName("person")
            .addMessage("Person")
            .addField(Type.Scalar.STRING, "firstName", 1)
            .addField(Type.Scalar.STRING, "lastName", 2)
            .build();

    static FileDescriptorSource personProtoDefinition() {
        return FileDescriptorSource.fromString(SCHEMA_PERSON.getName(), SCHEMA_PERSON.toString());
    }

    public static void registerSchema(RemoteCacheManager cacheContainer) {
        RemoteCache<Object, Object> metadataCache = cacheContainer
                .getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);
        metadataCache.put(SCHEMA_PERSON.getName(), SCHEMA_PERSON.toString());
    }

    @Produces
    @Named("additionalConfig")
    Map<String, String> additionalInfinispanConfig() {
        Map<String, String> config = new HashMap<>();
        config.put(MARSHALLER, ProtoStreamMarshaller.class.getName());
        if (OS.getCurrentOs().equals(OS.MAC_OS) || OS.getCurrentOs().equals(OS.WINDOWS)) {
            config.put(CLIENT_INTELLIGENCE, "BASIC");
        }
        config.put("infinispan.client.hotrod.cache.camel-infinispan.configuration", InfinispanCommonRoutes.LOCAL_CACHE_CONFIG);
        return config;
    }

    @Override
    protected Configuration getConfigurationBuilder() {
        ConfigurationBuilder clientBuilder = commonConfigurationBuilder();
        clientBuilder.marshaller(new ProtoStreamMarshaller());
        return clientBuilder.build();
    }

    /**
     * Common configuration builder which has all necessary info for connecting to the remote cache.
     */
    private ConfigurationBuilder commonConfigurationBuilder() {
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
                .realm(config.getValue("camel.component.infinispan.security-realm", String.class));
        return clientBuilder;
    }

    @Override
    protected InfinispanRemoteConfiguration getConfiguration() {
        InfinispanRemoteConfiguration configuration = new InfinispanRemoteConfiguration();
        configuration.setCacheContainerConfiguration(getConfigurationBuilder());
        return configuration;
    }

    public void configureWithPersonMarshaller(InfinispanRemoteConfiguration configuration) {
        ProtoStreamMarshaller marshaller = new ProtoStreamMarshaller();
        SerializationContext serializationContext = marshaller.getSerializationContext();
        serializationContext.registerProtoFiles(personProtoDefinition());
        serializationContext.registerMarshaller(new PersonMarshaller());

        Properties properties = new Properties();
        additionalInfinispanConfig().forEach((k, v) -> {
            properties.put(k, v);
        });
        ConfigurationBuilder clientBuilder = commonConfigurationBuilder();
        // apply properties from #additionalInfinispanConfig (needed eg. for configuring the `camel-infinispan` cache)
        clientBuilder.withProperties(properties);
        // apply the ProtoStreamMarshaller with PersonMarshaller configured
        clientBuilder.marshaller(marshaller);
        configuration.setCacheContainerConfiguration(clientBuilder.build());
    }

    /**
     * We are using custom camel component `infinispan-query` because we don't want to alter all other scenarios which
     * are supposed to be tested with the default configuration (modified only via camel quarkus properties).
     * This component is configured with needed Marshaller solely for testing the Infinispan querying.
     */
    @Produces
    @Named("infinispan-query")
    InfinispanComponent getInfinispanQuery() {
        InfinispanRemoteComponent infinispanComponent = new InfinispanRemoteComponent();
        // we don't wat quarkus-infinispan-client to get involved in this custom component
        infinispanComponent.setAutowiredEnabled(false);
        configureWithPersonMarshaller(infinispanComponent.getConfiguration());
        // we must create RemoteCacheManager by ourselves, as we need to apply the metadata for Person schema before it is used by InfinispanRemoteManager (via Camel's InfinispanRemoteEndpoint)
        // (otherwise Infinispan will respond something like `is not known. Please register its proto schema file first`)
        RemoteCacheManager cacheContainer = new RemoteCacheManager(
                infinispanComponent.getConfiguration().getCacheContainerConfiguration(), true);
        // register person.proto schema via metadata
        // (inspired by https://quarkus.io/guides/infinispan-client-reference#registering-protobuf-schemas-with-infinispan-server
        // and
        // https://github.com/quarkusio/quarkus/blob/3.25.0/extensions/infinispan-client/runtime/src/main/java/io/quarkus/infinispan/client/runtime/InfinispanClientProducer.java#L79)
        registerSchema(cacheContainer);
        // set directly the cacheContainer, so it is not re-created by endpoint (specifically InfinispanRemoteManager)
        infinispanComponent.getConfiguration().setCacheContainer(cacheContainer);
        return infinispanComponent;
    }
}
