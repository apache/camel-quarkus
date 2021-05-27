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
package org.apache.camel.quarkus.component.jta.it;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

public class JtaRoutes extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        onException(IllegalStateException.class).maximumRedeliveries(0).handled(true)
                .process(exchange -> {
                    Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    if (cause != null) {
                        exchange.getMessage().setBody(cause.getMessage());
                    }
                });

        from("direct:required")
                .transacted().transform().constant("required");

        from("direct:requires_new")
                .transacted("PROPAGATION_REQUIRES_NEW").transform().constant("requires_new");

        from("direct:mandatory")
                .transacted("PROPAGATION_MANDATORY").transform().constant("mandatory");

        from("direct:never")
                .transacted("PROPAGATION_NEVER").transform().constant("never");

        from("direct:supports")
                .transacted("PROPAGATION_SUPPORTS").transform().constant("supports");

        from("direct:not_supported")
                .transacted("PROPAGATION_NOT_SUPPORTED").transform().constant("not_supported");

        from("direct:jdbc")
                .transacted()
                .setHeader("message", body())
                .to("jms:queue:txTest?connectionFactory=#xaConnectionFactory&disableReplyTo=true")
                .transform().simple("insert into example(message, origin) values ('${body}', 'jdbc')")
                .to("jdbc:camel-ds?resetAutoCommit=false")
                .choice()
                .when(header("message").startsWith("fail"))
                .log("Failing forever with exception")
                .process(x -> {
                    throw new RuntimeException("Fail");
                })
                .otherwise()
                .transform().simple("${header.message} added")
                .endChoice();

        from("direct:sqltx")
                .transacted()
                .setHeader("message", body())
                .to("jms:queue:txTest?connectionFactory=#xaConnectionFactory&disableReplyTo=true")
                .to("sql:insert into example(message, origin) values (:#message, 'sqltx')")
                .choice()
                .when(header("message").startsWith("fail"))
                .log("Failing forever with exception")
                .process(x -> {
                    throw new RuntimeException("Fail");
                })
                .otherwise()
                .transform().simple("${header.message} added")
                .endChoice();

        from("jms:queue:txTest?connectionFactory=#xaConnectionFactory")
                .to("mock:txResult");
    }
}
