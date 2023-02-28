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
package org.apache.camel.quarkus.component.infinispan.common;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.infinispan.InfinispanConstants;
import org.apache.camel.component.infinispan.InfinispanOperation;
import org.apache.camel.component.infinispan.remote.InfinispanRemoteAggregationRepository;
import org.apache.camel.component.infinispan.remote.InfinispanRemoteConfiguration;
import org.apache.camel.component.infinispan.remote.InfinispanRemoteIdempotentRepository;
import org.infinispan.client.hotrod.configuration.Configuration;

import static org.apache.camel.quarkus.component.infinispan.common.InfinispanCommonResources.CACHE_NAME;

public abstract class InfinispanCommonRoutes extends RouteBuilder {
    public static final int COMPLETION_SIZE = 4;
    public static final String CORRELATOR_HEADER = "CORRELATOR_HEADER";

    @Override
    public void configure() {
        from("direct:clear")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.CLEAR)
                .setHeader(InfinispanConstants.KEY).constant("the-key")
                .toF("infinispan:%s", CACHE_NAME);

        from("direct:clearAsync")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.CLEARASYNC)
                .setHeader(InfinispanConstants.KEY).constant("the-key")
                .toF("infinispan:%s", CACHE_NAME);

        from("direct:compute")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.COMPUTE)
                .setHeader(InfinispanConstants.KEY).constant("the-key")
                .toF("infinispan:%s?remappingFunction=#customMappingFunction", CACHE_NAME);

        from("direct:computeAsync")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.COMPUTEASYNC)
                .setHeader(InfinispanConstants.KEY).constant("the-key")
                .toF("infinispan:%s?remappingFunction=#customMappingFunction", CACHE_NAME);

        from("direct:containsKey")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.CONTAINSKEY)
                .setHeader(InfinispanConstants.KEY).constant("the-key")
                .toF("infinispan:%s", CACHE_NAME);

        from("direct:containsValue")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.CONTAINSVALUE)
                .setHeader(InfinispanConstants.VALUE).body()
                .toF("infinispan:%s", CACHE_NAME);

        from("direct:get")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.GET)
                .toF("infinispan:%s", CACHE_NAME);

        from("direct:getOrDefault")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.GETORDEFAULT)
                .setHeader(InfinispanConstants.KEY).constant("the-key")
                .setHeader(InfinispanConstants.DEFAULT_VALUE).constant("default-value")
                .toF("infinispan:%s", CACHE_NAME);

        from("direct:put")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.PUT)
                .setHeader(InfinispanConstants.KEY).constant("the-key")
                .setHeader(InfinispanConstants.VALUE).body()
                .toF("infinispan:%s", CACHE_NAME);

        from("direct:putAsync")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.PUTASYNC)
                .setHeader(InfinispanConstants.KEY).constant("the-key")
                .setHeader(InfinispanConstants.VALUE).body()
                .toF("infinispan:%s", CACHE_NAME);

        from("direct:putAll")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.PUTALL)
                .setHeader(InfinispanConstants.MAP).body()
                .toF("infinispan:%s", CACHE_NAME);

        from("direct:putAllAsync")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.PUTALLASYNC)
                .setHeader(InfinispanConstants.MAP).body()
                .toF("infinispan:%s", CACHE_NAME);

        from("direct:putIfAbsent")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.PUTIFABSENT)
                .setHeader(InfinispanConstants.KEY).constant("the-key")
                .setHeader(InfinispanConstants.VALUE).body()
                .toF("infinispan:%s", CACHE_NAME);

        from("direct:putIfAbsentAsync")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.PUTIFABSENTASYNC)
                .setHeader(InfinispanConstants.KEY).constant("the-key")
                .setHeader(InfinispanConstants.VALUE).body()
                .toF("infinispan:%s", CACHE_NAME);

        from("direct:query")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.QUERY)
                .toF("infinispan:%s", CACHE_NAME);

        from("direct:remove")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.REMOVE)
                .setHeader(InfinispanConstants.KEY).constant("the-key")
                .toF("infinispan:%s", CACHE_NAME);

        from("direct:removeAsync")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.REMOVEASYNC)
                .setHeader(InfinispanConstants.KEY).constant("the-key")
                .toF("infinispan:%s", CACHE_NAME);

        from("direct:replace")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.REPLACE)
                .setHeader(InfinispanConstants.KEY).constant("the-key")
                .setHeader(InfinispanConstants.VALUE).body()
                .toF("infinispan:%s", CACHE_NAME);

        from("direct:replaceAsync")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.REPLACEASYNC)
                .setHeader(InfinispanConstants.KEY).constant("the-key")
                .setHeader(InfinispanConstants.VALUE).body()
                .toF("infinispan:%s", CACHE_NAME);

        from("direct:size")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.SIZE)
                .setHeader(InfinispanConstants.KEY).constant("the-key")
                .toF("infinispan:%s", CACHE_NAME);

        from("direct:stats")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.STATS)
                .toF("infinispan:%s", CACHE_NAME);

        fromF("infinispan:%s?eventTypes=CLIENT_CACHE_ENTRY_CREATED", CACHE_NAME)
                .id("infinispan-events")
                .autoStartup(false)
                .to("mock:resultCreated");

        from("direct:aggregation")
                .aggregate(header(CORRELATOR_HEADER))
                .aggregationRepository(createAggregationRepository())
                .aggregationStrategy(createAggregationStrategy())
                .completionSize(COMPLETION_SIZE)
                .to("mock:aggregationResult");

        from("direct:idempotent")
                .idempotentConsumer(header("MessageID"), createIdempotentRepository())
                .to("mock:idempotentResult");

        fromF("infinispan:%s?customListener=#customListener", CACHE_NAME)
                .id("infinispan-custom-listener")
                .autoStartup(false)
                .to("mock:resultCustomListener");
    }

    protected abstract Configuration getConfigurationBuilder();

    protected abstract InfinispanRemoteConfiguration getConfiguration();

    private InfinispanRemoteIdempotentRepository createIdempotentRepository() {
        InfinispanRemoteConfiguration configuration = getConfiguration();
        InfinispanRemoteIdempotentRepository repository = new InfinispanRemoteIdempotentRepository(CACHE_NAME);
        repository.setConfiguration(configuration);
        return repository;
    }

    private InfinispanRemoteAggregationRepository createAggregationRepository() {
        InfinispanRemoteAggregationRepository aggregationRepository = new InfinispanRemoteAggregationRepository(CACHE_NAME);
        InfinispanRemoteConfiguration configuration = getConfiguration();
        aggregationRepository.setConfiguration(configuration);
        return aggregationRepository;
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
}
