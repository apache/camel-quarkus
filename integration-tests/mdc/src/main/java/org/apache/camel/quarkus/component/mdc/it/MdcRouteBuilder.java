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
package org.apache.camel.quarkus.component.mdc.it;

import org.apache.camel.builder.RouteBuilder;
import org.slf4j.MDC;

public class MdcRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        // Route for testing custom headers
        from("direct:customHeader")
                .setHeader("myHeader", constant("HELO"))
                .process(exchange -> {
                    exchange.getIn().setBody(MDC.get("myHeader"));
                })
                .log("Custom header route done");

        // Route for testing default MDC fields
        from("direct:defaultFields")
                .process(exchange -> {
                    StringBuilder result = new StringBuilder();
                    result.append("exchangeId:").append(MDC.get("camel.exchangeId")).append(",");
                    result.append("messageId:").append(MDC.get("camel.messageId")).append(",");
                    result.append("routeId:").append(MDC.get("camel.routeId")).append(",");
                    result.append("contextId:").append(MDC.get("camel.contextId")).append(",");
                    result.append("threadId:").append(MDC.get("camel.threadId"));
                    exchange.getIn().setBody(result.toString());
                })
                .log("Default fields route done");

        // Route for testing properties
        from("direct:properties")
                .setProperty("prop1", constant("property1"))
                .setProperty("prop2", constant("property2"))
                .process(exchange -> {
                    StringBuilder result = new StringBuilder();
                    String p1 = MDC.get("prop1");
                    String p2 = MDC.get("prop2");
                    result.append("prop1:").append(p1 != null ? p1 : "null").append(",");
                    result.append("prop2:").append(p2 != null ? p2 : "null");
                    exchange.getIn().setBody(result.toString());
                })
                .log("Properties route done");

        // Route for testing async processing
        from("direct:async")
                .setHeader("asyncHeader", constant("asyncValue"))
                .setProperty("asyncProp", constant("asyncPropValue"))
                .process(exchange -> exchange.setProperty("callingThread", Thread.currentThread().getName()))
                .threads(2)
                .process(exchange -> {
                    StringBuilder result = new StringBuilder();
                    String ah = MDC.get("asyncHeader");
                    String ap = MDC.get("asyncProp");
                    String threadId = MDC.get("camel.threadId");
                    result.append("asyncHeader:").append(ah != null ? ah : "null").append("\n");
                    result.append("asyncProp:").append(ap != null ? ap : "null").append("\n");
                    result.append("threadId:").append(threadId != null ? threadId : "null").append("\n");
                    result.append("callingThread:").append(exchange.getProperty("callingThread", String.class));
                    exchange.getIn().setBody(result.toString());
                })
                .log("Async route done");

        // Route for testing intercept + bean processor MDC propagation
        from("direct:interceptBean")
                .setHeader("interceptHeader", constant("interceptValue"))
                .process(exchange -> {
                    // Processor acting as the "bean": assert MDC is populated here
                    StringBuilder result = new StringBuilder();
                    String ih = MDC.get("interceptHeader");
                    String exchangeId = MDC.get("camel.exchangeId");
                    String routeId = MDC.get("camel.routeId");
                    String contextId = MDC.get("camel.contextId");
                    result.append("interceptHeader:").append(ih != null ? ih : "null").append(",");
                    result.append("exchangeId:").append(exchangeId != null ? "present" : "null").append(",");
                    result.append("routeId:").append(routeId != null ? "present" : "null").append(",");
                    result.append("contextId:").append(contextId != null ? "present" : "null");
                    exchange.getIn().setBody(result.toString());
                })
                .log("Intercept-bean route done");
    }
}
