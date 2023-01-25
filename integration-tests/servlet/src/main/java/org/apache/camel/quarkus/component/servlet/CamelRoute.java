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
package org.apache.camel.quarkus.component.servlet;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class CamelRoute extends RouteBuilder {

    @Override
    public void configure() {
        // by default the camel-quarkus-rest component sets platform-http
        // as the component that provides the transport and since here we
        // are testing the servlet component. we have to force it
        restConfiguration()
                .component("servlet");

        rest()
                .get("/rest-get")
                .to("direct:echoMethodPath")

                .post("/rest-post")
                .to("direct:echoMethodPath");

        from("servlet://hello?matchOnUriPrefix=true")
                .setBody(constant("GET: /hello"));

        from("servlet://custom?servletName=my-named-servlet")
                .setBody(constant("GET: /custom"));

        from("servlet://favorite?servletName=my-favorite-servlet")
                .setBody(constant("GET: /favorite"));

        from("direct:echoMethodPath")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        exchange.toString();
                    }
                })
                .setBody().simple("${header.CamelHttpMethod}: ${header.CamelServletContextPath}");
    }

}
