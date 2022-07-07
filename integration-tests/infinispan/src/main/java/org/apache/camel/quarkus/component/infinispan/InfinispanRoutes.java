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
import java.util.Set;
import java.util.function.BiFunction;

import javax.inject.Named;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.infinispan.InfinispanConstants;
import org.apache.camel.component.infinispan.InfinispanOperation;
import org.apache.camel.component.infinispan.remote.InfinispanRemoteAggregationRepository;
import org.apache.camel.component.infinispan.remote.InfinispanRemoteComponent;
import org.apache.camel.component.infinispan.remote.InfinispanRemoteComponentConfigurer;
import org.apache.camel.component.infinispan.remote.InfinispanRemoteConfiguration;
import org.apache.camel.component.infinispan.remote.InfinispanRemoteCustomListener;
import org.apache.camel.component.infinispan.remote.InfinispanRemoteIdempotentRepository;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;
import org.infinispan.commons.marshall.StringMarshaller;

import static org.apache.camel.quarkus.component.infinispan.InfinispanResources.CACHE_NAME_CAMEL;
import static org.apache.camel.quarkus.component.infinispan.InfinispanResources.CACHE_NAME_QUARKUS;

public class InfinispanRoutes extends RouteBuilder {
    public static final int COMPLETION_SIZE = 4;
    public static final String CORRELATOR_HEADER = "CORRELATOR_HEADER";

    @Override
    public void configure() {
        from("direct:clear")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.CLEAR)
                .setHeader(InfinispanConstants.KEY).constant("the-key")
                .toD("${header.component}:${header.cacheName}");

        from("direct:clearAsync")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.CLEARASYNC)
                .setHeader(InfinispanConstants.KEY).constant("the-key")
                .toD("${header.component}:${header.cacheName}");

        from("direct:compute")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.COMPUTE)
                .setHeader(InfinispanConstants.KEY).constant("the-key")
                .toD("${header.component}:${header.cacheName}?remappingFunction=#customMappingFunction");

        from("direct:computeAsync")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.COMPUTEASYNC)
                .setHeader(InfinispanConstants.KEY).constant("the-key")
                .toD("${header.component}:${header.cacheName}?remappingFunction=#customMappingFunction");

        from("direct:containsKey")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.CONTAINSKEY)
                .setHeader(InfinispanConstants.KEY).constant("the-key")
                .toD("${header.component}:${header.cacheName}");

        from("direct:containsValue")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.CONTAINSVALUE)
                .setHeader(InfinispanConstants.VALUE).body()
                .toD("${header.component}:${header.cacheName}");

        from("direct:get")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.GET)
                .toD("${header.component}:${header.cacheName}");

        from("direct:getOrDefault")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.GETORDEFAULT)
                .setHeader(InfinispanConstants.KEY).constant("the-key")
                .setHeader(InfinispanConstants.DEFAULT_VALUE).constant("default-value")
                .toD("${header.component}:${header.cacheName}");

        from("direct:put")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.PUT)
                .setHeader(InfinispanConstants.KEY).constant("the-key")
                .setHeader(InfinispanConstants.VALUE).body()
                .toD("${header.component}:${header.cacheName}");

        from("direct:putAsync")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.PUTASYNC)
                .setHeader(InfinispanConstants.KEY).constant("the-key")
                .setHeader(InfinispanConstants.VALUE).body()
                .toD("${header.component}:${header.cacheName}");

        from("direct:putAll")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.PUTALL)
                .setHeader(InfinispanConstants.MAP).body()
                .toD("${header.component}:${header.cacheName}");

        from("direct:putAllAsync")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.PUTALLASYNC)
                .setHeader(InfinispanConstants.MAP).body()
                .toD("${header.component}:${header.cacheName}");

        from("direct:putIfAbsent")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.PUTIFABSENT)
                .setHeader(InfinispanConstants.KEY).constant("the-key")
                .setHeader(InfinispanConstants.VALUE).body()
                .toD("${header.component}:${header.cacheName}");

        from("direct:putIfAbsentAsync")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.PUTIFABSENTASYNC)
                .setHeader(InfinispanConstants.KEY).constant("the-key")
                .setHeader(InfinispanConstants.VALUE).body()
                .toD("${header.component}:${header.cacheName}");

        from("direct:query")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.QUERY)
                .toD("${header.component}:${header.cacheName}");

        from("direct:remove")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.REMOVE)
                .setHeader(InfinispanConstants.KEY).constant("the-key")
                .toD("${header.component}:${header.cacheName}");

        from("direct:removeAsync")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.REMOVEASYNC)
                .setHeader(InfinispanConstants.KEY).constant("the-key")
                .toD("${header.component}:${header.cacheName}");

        from("direct:replace")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.REPLACE)
                .setHeader(InfinispanConstants.KEY).constant("the-key")
                .setHeader(InfinispanConstants.VALUE).body()
                .toD("${header.component}:${header.cacheName}");

        from("direct:replaceAsync")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.REPLACEASYNC)
                .setHeader(InfinispanConstants.KEY).constant("the-key")
                .setHeader(InfinispanConstants.VALUE).body()
                .toD("${header.component}:${header.cacheName}");

        from("direct:size")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.SIZE)
                .setHeader(InfinispanConstants.KEY).constant("the-key")
                .toD("${header.component}:${header.cacheName}");

        from("direct:stats")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.STATS)
                .toD("${header.component}:${header.cacheName}");

        from("infinispan:camel?eventTypes=CLIENT_CACHE_ENTRY_CREATED")
                .id("infinispan-events")
                .autoStartup(false)
                .to("mock:camelResultCreated");

        from("direct:camelAggregation")
                .aggregate(header(CORRELATOR_HEADER))
                .aggregationRepository(createAggregationRepository("infinispan"))
                .aggregationStrategy(createAggregationStrategy())
                .completionSize(COMPLETION_SIZE)
                .to("mock:camelAggregationResult");

        from("direct:quarkusAggregation")
                .aggregate(header(CORRELATOR_HEADER))
                .aggregationRepository(createAggregationRepository("infinispan-quarkus"))
                .aggregationStrategy(createAggregationStrategy())
                .completionSize(COMPLETION_SIZE)
                .to("mock:quarkusAggregationResult");

        from("direct:camelIdempotent")
                .idempotentConsumer(header("MessageID"), createIdempotentRepository("infinispan"))
                .to("mock:camelResultIdempotent");

        from("direct:quarkusIdempotent")
                .idempotentConsumer(header("MessageID"), createIdempotentRepository("infinispan-quarkus"))
                .to("mock:quarkusResultIdempotent");

        from("infinispan-quarkus:quarkus?eventTypes=CLIENT_CACHE_ENTRY_CREATED")
                .id("infinispan-quarkus-events")
                .autoStartup(false)
                .to("mock:quarkusResultCreated");

        from("infinispan:camel?customListener=#customListener")
                .id("infinispan-custom-listener")
                .autoStartup(false)
                .to("mock:camelResultCustomListener");

        from("infinispan-quarkus:quarkus?customListener=#customListener")
                .id("infinispan-quarkus-custom-listener")
                .autoStartup(false)
                .to("mock:quarkusResultCustomListener");
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

    @Named("customMappingFunction")
    public BiFunction<String, String, String> mappingFunction() {
        return (k, v) -> v + "-remapped";
    }

    @Named("customListener")
    public InfinispanRemoteCustomListener customListener() {
        return new CustomListener();
    }

    private InfinispanRemoteIdempotentRepository createIdempotentRepository(String componentName) {
        String cacheName = componentName.equals("infinispan") ? CACHE_NAME_CAMEL : CACHE_NAME_QUARKUS;
        InfinispanRemoteConfiguration configuration = getConfiguration(componentName);
        InfinispanRemoteIdempotentRepository repository = new InfinispanRemoteIdempotentRepository(cacheName);
        repository.setConfiguration(configuration);
        return repository;
    }

    private InfinispanRemoteAggregationRepository createAggregationRepository(String componentName) {
        String cacheName = componentName.equals("infinispan") ? CACHE_NAME_CAMEL : CACHE_NAME_QUARKUS;
        InfinispanRemoteAggregationRepository aggregationRepository = new InfinispanRemoteAggregationRepository(cacheName);
        InfinispanRemoteConfiguration configuration = getConfiguration(componentName);
        aggregationRepository.setConfiguration(configuration);
        return aggregationRepository;
    }

    private InfinispanRemoteConfiguration getConfiguration(String componentName) {
        CamelContext camelContext = getCamelContext();
        InfinispanRemoteComponent component = camelContext.getComponent(componentName, InfinispanRemoteComponent.class);
        InfinispanRemoteConfiguration configuration = component.getConfiguration().clone();
        configuration.setCacheContainerConfiguration(getConfigurationBuilder());
        Set<RemoteCacheManager> beans = camelContext.getRegistry().findByType(RemoteCacheManager.class);
        RemoteCacheManager cacheManager = beans.iterator().next();
        configuration.setCacheContainer(cacheManager);
        return configuration;
    }

    private Configuration getConfigurationBuilder() {
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
                .marshaller(new StringMarshaller(StandardCharsets.UTF_8));

        return clientBuilder.build();
    }

    private AggregationStrategy createAggregationStrategy() {
        return (oldExchange, newExchange) -> {
            if (oldExchange == null) {
                return newExchange;
            } else {
                Integer n = newExchange.getIn().getBody(Integer.class);
                Integer o = oldExchange.getIn().getBody(Integer.class);
                Integer v = (o == null ? 0 : o) + (n == null ? 0 : n);
                oldExchange.getIn().setBody(v, Integer.class);
                return oldExchange;
            }
        };
    }

    @ClientListener
    static final class CustomListener extends InfinispanRemoteCustomListener {

        @ClientCacheEntryCreated
        public void entryCreated(ClientCacheEntryCreatedEvent<?> event) {
            if (isAccepted(event.getType())) {
                getEventProcessor().processEvent(
                        event.getType().toString(),
                        getCacheName(),
                        event.getKey(),
                        null,
                        null);
            }
        }
    }
}
