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
package org.apache.camel.quarkus.component.lra.it;

import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.SagaCompletionMode;
import org.apache.camel.model.SagaPropagation;
import org.apache.camel.quarkus.component.lra.it.service.CreditService;
import org.apache.camel.quarkus.component.lra.it.service.OrderManagerService;

@ApplicationScoped
public class LraRoutes extends RouteBuilder {

    @Inject
    CreditService creditService;

    @Inject
    OrderManagerService orderManagerService;

    @Override
    public void configure() throws Exception {
        from("direct:saga")
                .saga().propagation(SagaPropagation.REQUIRES_NEW)
                .log("Creating a new order")
                .to("direct:newOrder")
                .log("Taking the credit")
                .to("direct:reserveCredit")
                .log("Finalizing")
                .to("direct:finalize")
                .log("Done!");

        // Order service
        from("direct:newOrder")
                .saga()
                .propagation(SagaPropagation.MANDATORY)
                .compensation("direct:cancelOrder")
                .transform().header(Exchange.SAGA_LONG_RUNNING_ACTION)
                .bean(orderManagerService, "newOrder")
                .log("Order ${body} created");

        from("direct:cancelOrder")
                .transform().header(Exchange.SAGA_LONG_RUNNING_ACTION)
                .bean(orderManagerService, "cancelOrder")
                .log("Order ${body} cancelled");

        // Credit service
        from("direct:reserveCredit")
                .saga()
                .propagation(SagaPropagation.MANDATORY)
                .compensation("direct:refundCredit")
                .transform().header(Exchange.SAGA_LONG_RUNNING_ACTION)
                .bean(creditService, "reserveCredit")
                .log("Credit ${header.amount} reserved in action ${body}");

        from("direct:refundCredit")
                .transform().header(Exchange.SAGA_LONG_RUNNING_ACTION)
                .bean(creditService, "refundCredit")
                .log("Credit for action ${body} refunded");

        // Final actions
        from("direct:finalize")
                .saga().propagation(SagaPropagation.NOT_SUPPORTED)
                .choice()
                .when(header("fail").isEqualTo(true))
                .process(x -> {
                    throw new Exception("fail");
                })
                .end();

        // ManualSaga
        from("direct:manualSaga")
                .saga()
                .completionMode(SagaCompletionMode.MANUAL)
                .timeout(1, TimeUnit.SECONDS)
                .option("id", header("myid"))
                .completion("direct:complete")
                .compensation("direct:compensate")
                .to("mock:endpoint")
                .choice()
                .when(body().isEqualTo("fail"))
                .to("saga:compensate")
                .when(body().isNotEqualTo("timeout"))
                .to("saga:complete")
                .end();

        from("direct:complete")
                .setBody(constant("complete"))
                .to("mock:complete");

        from("direct:compensate")
                .setBody(constant("compensate"))
                .to("mock:compensate");

    }
}
