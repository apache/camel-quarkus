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
package org.apache.camel.quarkus.component.opentelemetry.it;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

public class OpenTelemetryRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("platform-http:/opentelemetry/test/trace?httpMethodRestrict=GET")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
                .setBody(constant("GET: /opentelemetry/test/trace"));

        from("platform-http:/opentelemetry/test/trace/filtered")
                .setBody(constant("GET: /opentelemetry/test/trace/filtered"));

        from("direct:start")
                .setBody().constant("Traced direct:start");

        from("direct:greet")
                .to("bean:greetingsBean");

        from("timer:filtered?repeatCount=5&delay=-1")
                .setBody().constant("Route filtered from tracing");

        from("direct:jdbcQuery")
                .to("bean:jdbcQueryBean");

        from("platform-http:/greeting")
                .log("Received /greeting request for component ${header.httpComponent}")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) {
                        String baseUrl = "http://localhost";
                        String httpComponent = exchange.getMessage().getHeader("httpComponent", String.class);
                        if (httpComponent.equals("http")) {
                            exchange.setVariable("httpUriPrefix", baseUrl);
                        } else {
                            exchange.setVariable("httpUriPrefix", httpComponent + ":" + baseUrl);
                        }
                    }
                })
                .removeHeaders("*")
                .toD("${variable.httpUriPrefix}:{{quarkus.http.test-port}}/greeting-provider");

        from("platform-http:/greeting-provider")
                .log("Received at greeting-provider: ${body}")
                .setBody(constant("Hello From Camel Quarkus!"));
    }
}
