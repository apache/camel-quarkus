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
import org.apache.camel.processor.idempotent.kafka.KafkaIdempotentRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public class CamelKafkaRoutes extends RouteBuilder {
    @ConfigProperty(name = "camel.component.kafka.brokers")
    String brokers;

    @Produces
    @ApplicationScoped
    @Named("kafkaIdempotentRepository")
    KafkaIdempotentRepository kafkaIdempotentRepository() {
        return new KafkaIdempotentRepository("idempotent-topic", brokers);
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
    }
}
