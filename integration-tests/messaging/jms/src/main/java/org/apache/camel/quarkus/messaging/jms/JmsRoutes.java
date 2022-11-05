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
package org.apache.camel.quarkus.messaging.jms;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.TransactionManager;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.quarkus.component.messaging.it.util.scheme.ComponentScheme;

@ApplicationScoped
public class JmsRoutes extends RouteBuilder {

    @Inject
    ComponentScheme componentScheme;

    @Inject
    TransactionManager transactionManager;

    @Override
    public void configure() throws Exception {

        fromF("%s:queue:transferExchange?transferExchange=true", componentScheme)
                .to("mock:transferExchangeResult");

        fromF("%s:queue:transferException?transferException=true", componentScheme)
                .throwException(new IllegalStateException("Forced exception"));

        from("direct:computedDestination")
                .bean("destinationHeaderSetter")
                .toF("%s:queue:override", componentScheme);

        fromF("%s:queue:xa", componentScheme)
                .log("Received message ${body}")
                .to("mock:xaResult");

        from("direct:xa")
                .transacted()
                .process(x -> {
                    transactionManager.getTransaction().enlistResource(new DummyXAResource());
                })
                .toF("%s:queue:xa?disableReplyTo=true", componentScheme)
                .choice()
                .when(body().startsWith("fail"))
                .log("Forced to rollback")
                .process(x -> {
                    transactionManager.setRollbackOnly();
                })
                .otherwise()
                .log("Message added: ${body}")
                .endChoice();
    }
}
