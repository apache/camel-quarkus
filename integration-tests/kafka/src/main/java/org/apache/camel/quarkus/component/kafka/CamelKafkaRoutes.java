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
package org.apache.camel.quarkus.component.kafka;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.component.kafka.consumer.KafkaManualCommit;
import org.apache.camel.processor.idempotent.kafka.KafkaIdempotentRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public class CamelKafkaRoutes extends RouteBuilder {

    private final static String KAFKA_CONSUMER_MANUAL_COMMIT = "kafka:manual-commit-topic"
            + "?groupId=group1&sessionTimeoutMs=30000&autoCommitEnable=false"
            + "&allowManualCommit=true&autoOffsetReset=earliest";

    private final static String SEDA_FOO = "seda:foo";
    private final static String SEDA_SERIALIZER = "seda:serializer";
    private final static String SEDA_HEADER_PROPAGATION = "seda:propagation";

    @ConfigProperty(name = "camel.component.kafka.brokers")
    String brokers;

    @Produces
    @ApplicationScoped
    @Named("kafkaIdempotentRepository")
    KafkaIdempotentRepository kafkaIdempotentRepository() {
        return new KafkaIdempotentRepository("idempotent-topic", brokers);
    }

    @Produces
    @ApplicationScoped
    @Named("customHeaderDeserializer")
    CustomHeaderDeserializer customHeaderDeserializer() {
        return new CustomHeaderDeserializer();
    }

    @Override
    public void configure() throws Exception {
        from("kafka:inbound?autoOffsetReset=earliest")
                .to("log:kafka")
                .to("kafka:outbound");

        from("direct:idempotent")
                .idempotentConsumer(header("id"))
                .messageIdRepositoryRef("kafkaIdempotentRepository")
                .to("mock:idempotent-results")
                .end();

        CounterRoutePolicy counterRoutePolicy = new CounterRoutePolicy();

        // Kafka consumer that use Manual commit
        // it manually commits only once every 2 messages received, so that we could test redelivery of uncommitted messages
        from(KAFKA_CONSUMER_MANUAL_COMMIT)
                .routeId("foo")
                .routePolicy(counterRoutePolicy)
                .to(SEDA_FOO)
                .process(e -> {
                    int counter = counterRoutePolicy.getCounter();
                    if (counter % 2 != 0) {
                        KafkaManualCommit manual = e.getMessage().getHeader(KafkaConstants.MANUAL_COMMIT,
                                KafkaManualCommit.class);
                        manual.commit();
                    }
                });

        // By default, keyDeserializer & valueDeserializer == org.apache.kafka.common.serialization.StringDeserializer
        // and valueSerializer & keySerializer == org.apache.kafka.common.serialization.StringSerializer
        // the idea here is to test setting different kinds of Deserializers
        from("kafka:test-serializer?autoOffsetReset=earliest" +
                "&keyDeserializer=org.apache.kafka.common.serialization.IntegerDeserializer" +
                "&valueDeserializer=org.apache.kafka.common.serialization.DoubleDeserializer")
                        .to(SEDA_SERIALIZER);

        // Header Propagation using CustomHeaderDeserialize
        from("kafka:test-propagation?headerDeserializer=#customHeaderDeserializer")
                .to(SEDA_HEADER_PROPAGATION);

    }
}
