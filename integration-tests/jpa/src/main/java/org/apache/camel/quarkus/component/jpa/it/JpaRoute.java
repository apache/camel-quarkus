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
package org.apache.camel.quarkus.component.jpa.it;

import java.util.Collections;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.narayana.jta.RunOptions;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jpa.TransactionStrategy;
import org.apache.camel.processor.idempotent.jpa.JpaMessageIdRepository;
import org.apache.camel.quarkus.component.jpa.it.model.Fruit;

import static io.quarkus.narayana.jta.QuarkusTransaction.runOptions;

@ApplicationScoped
public class JpaRoute extends RouteBuilder {

    @Inject
    EntityManagerFactory entityManagerFactory;

    @Override
    public void configure() throws Exception {
        String jpaEndpoint = "jpa:" + Fruit.class.getName();
        bindToRegistry("parameters", Collections.singletonMap("fruitName", "${body}"));

        from("direct:findAll")
                .to(jpaEndpoint + "?query=select f from " + Fruit.class.getName() + " f");
        from("direct:findById")
                .to(jpaEndpoint + "?findEntity=true");
        from("direct:namedQuery")
                .to(jpaEndpoint + "?namedQuery=findByName&parameters=#parameters");
        from("direct:nativeQuery")
                .to(jpaEndpoint + "?resultClass=org.apache.camel.quarkus.component.jpa.it.model.Fruit" +
                        "&nativeQuery=SELECT * FROM fruit WHERE id = :id");
        from("direct:store")
                .to(jpaEndpoint);
        from("direct:remove")
                .to(jpaEndpoint + "?remove=true");

        from("direct:transaction")
                .transacted()
                .to("direct:store")
                .process(x -> {
                    if (x.getIn().getHeader("rollback", false, Boolean.class)) {
                        throw new RuntimeException("forced exception");
                    }
                });

        from(jpaEndpoint + "?namedQuery=unprocessed")
                .log("Consume fruit: ${body}")
                .process(x -> {
                    if (!x.getIn().getHeader("preConsumed", Boolean.class)) {
                        throw new AssertionError("preConsumed method has not been executed");
                    }
                })
                .to("mock:processed");

        from("direct:idempotent")
                .idempotentConsumer(
                        header("messageId"),
                        new JpaMessageIdRepository(entityManagerFactory, new TransactionStrategy() {
                            @Override
                            public void executeInTransaction(Runnable runnable) {
                                QuarkusTransaction.run(runOptions().semantic(RunOptions.Semantic.JOIN_EXISTING), runnable);
                            }
                        }, "idempotentProcessor"))
                .log("Consumes messageId: ${header.messageId}")
                .to("mock:idempotent");

        from("direct:idempotentLog")
                .enrich("jpa:MessageProcessed?consumeDelete=false&query=select f from MessageProcessed f");

    }
}
