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
package org.apache.camel.quarkus.component.saga.it;

import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.SagaCompletionMode;
import org.apache.camel.model.SagaPropagation;
import org.apache.camel.quarkus.component.saga.it.lra.LraCreditService;
import org.apache.camel.quarkus.component.saga.it.lra.LraService;
import org.apache.camel.quarkus.component.saga.it.lra.LraTicketService;
import org.apache.camel.saga.CamelSagaService;
import org.apache.camel.saga.InMemorySagaService;

@ApplicationScoped
public class SagaRoute extends RouteBuilder {
    @Inject
    OrderManagerService orderManagerService;

    @Inject
    CreditService creditService;

    @Inject
    LraService lraService;

    @Inject
    LraCreditService lraCreditService;

    @Inject
    LraTicketService lraTicketService;

    @Override
    public void configure() throws Exception {
        CamelSagaService sagaService = new InMemorySagaService();
        getContext().addService(sagaService);

        from("direct:saga").saga().propagation(SagaPropagation.REQUIRES_NEW).log("Creating a new order")
                .to("direct:newOrder").log("Taking the credit")
                .to("direct:reserveCredit").log("Finalizing").to("direct:finalize").log("Done!");

        // Order service

        from("direct:newOrder").saga().propagation(SagaPropagation.MANDATORY).compensation("direct:cancelOrder")
                .transform().header(Exchange.SAGA_LONG_RUNNING_ACTION)
                .bean(orderManagerService, "newOrder").log("Order ${body} created");

        from("direct:cancelOrder").transform().header(Exchange.SAGA_LONG_RUNNING_ACTION)
                .bean(orderManagerService, "cancelOrder").log("Order ${body} cancelled");

        // Credit service

        from("direct:reserveCredit").saga().propagation(SagaPropagation.MANDATORY).compensation("direct:refundCredit")
                .transform().header(Exchange.SAGA_LONG_RUNNING_ACTION)
                .bean(creditService, "reserveCredit").log("Credit ${header.amount} reserved in action ${body}");

        from("direct:refundCredit").transform().header(Exchange.SAGA_LONG_RUNNING_ACTION)
                .bean(creditService, "refundCredit").log("Credit for action ${body} refunded");

        // Final actions
        from("direct:finalize").saga().propagation(SagaPropagation.MANDATORY).choice()
                .when(header("fail").isEqualTo(true)).to("saga:COMPENSATE").end();

        // ---------------------- saga with JMS using custom ids / timeouts / completion / Long-Running-Action header

        from("direct:lraSaga")
                .saga()
                .compensation("direct:lraCancelOrder")
                .completion("direct:lraCompleted")
                .log("Executing saga #${header.id} with LRA ${header.Long-Running-Action}")
                .setHeader("payFor", constant("train"))
                .setHeader("amount", header("trainCost"))
                .to("jms:queue:train?exchangePattern=InOut" +
                        "&replyTo=train.reply")
                .log("train seat reserved for saga #${header.id} with payment transaction: ${body}")
                .setHeader("payFor", constant("flight"))
                .setHeader("amount", header("flightCost"))
                .to("jms:queue:flight?exchangePattern=InOut" +
                        "&replyTo=flight.reply")
                .log("flight booked for saga #${header.id} with payment transaction: ${body}")
                .setBody(header("Long-Running-Action"))
                .end();

        from("direct:lraCancelOrder")
                .log("Transaction ${header.Long-Running-Action} has been cancelled due to flight or train insufficient payment, refunding all tickets")
                .bean(lraCreditService, "refundCredit")
                .bean(lraTicketService, "setTicketsRefunded")
                .log("Credit for action ${body} refunded");

        from("direct:lraCompleted")
                .log("Transaction ${header.Long-Running-Action} has been completed.")
                .bean(lraTicketService, "setTicketsReserved");

        //train
        from("jms:queue:train")
                .saga()
                .propagation(SagaPropagation.MANDATORY)
                .option("id", header("id"))
                .compensation("direct:lraTrainCancelPurchase")
                .log("Buying train #${header.id}")
                .to("jms:queue:payment?exchangePattern=InOut" +
                        "&replyTo=payment.train.reply")
                .log("Payment for train #${header.id} done with transaction ${body}")
                .end();

        from("direct:lraTrainCancelPurchase")
                .log("Train purchase #${header.id} has been cancelled due to payment failure");

        //flight
        from("jms:queue:flight")
                .saga()
                .propagation(SagaPropagation.MANDATORY)
                .option("id", header("id"))
                .compensation("direct:lraFlightCancelPurchase")
                .log("Buying flight #${header.id}")
                .to("jms:queue:payment?exchangePattern=InOut" +
                        "&replyTo={payment.flight.reply")
                .log("Payment for flight #${header.id} done with transaction ${body}")
                .end();

        from("direct:lraFlightCancelPurchase")
                .log("Flight purchase #${header.id} has been cancelled due to payment failure");

        from("jms:queue:payment")
                .routeId("payment-service")
                .saga()
                .propagation(SagaPropagation.MANDATORY)
                .option("id", header("id"))
                .option("payFor", header("payFor"))
                .compensation("direct:lraCancelPayment")
                .log("Paying ${header.payFor} (${header.amount}) for order #${header.id}")
                .bean(lraCreditService, "reserveCredit")
                .setBody(header("JMSCorrelationID"))
                .log("Payment ${header.payFor} done for order #${header.id} with payment transaction ${body}")
                .end();

        from("direct:lraCancelPayment")
                .routeId("payment-cancel")
                .choice()
                .when(header("payFor").contains("train")).bean(lraTicketService, "setTrainError")
                .when(header("payFor").contains("flight")).bean(lraTicketService, "setFlightError")
                .endChoice()
                .log("Payment for order #${header.id} did not finish (insufficient credit)");

        // ----------------- timeout ------------------------------

        from("direct:newOrderTimeout5sec")
                .saga()
                .timeout(5, TimeUnit.SECONDS) // newOrder requires that the saga is completed within 5 seconds
                .propagation(SagaPropagation.REQUIRES_NEW)
                .compensation("direct:cancelOrderTimeout5sec")
                .bean(lraService, "sleep")
                .setBody(constant("success"))
                .log("Order ${body} created");

        from("direct:cancelOrderTimeout5sec")
                .saga()
                .propagation(SagaPropagation.MANDATORY)
                .setBody(constant("failure"));

        // ----------------- manual ----------------------------------

        from("direct:manualSaga")
                .saga()
                .completionMode(SagaCompletionMode.MANUAL)
                .completion("seda:manualSagaComplete")
                .to("seda:manualSagaProcessOrder");

        from("seda:manualSagaProcessOrder") // an asynchronous callback
                .saga()
                .propagation(SagaPropagation.MANDATORY)
                .log("Processing manual saga order with complete set to ${header.shouldComplete}")
                .choice()
                .when(header("shouldComplete").isEqualTo("true"))
                .to("saga:complete") // complete the current saga manually (saga component)
                .end();

        from("seda:manualSagaComplete") // an asynchronous callback
                .log("Manual saga marked as completed")
                .bean(lraService, "complete");

    }
}
