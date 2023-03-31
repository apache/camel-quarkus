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
package org.apache.camel.quarkus.component.jdbc;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.jboss.logging.Logger;

@ApplicationScoped
public class JdbcRoutes extends RouteBuilder {
    private static final Logger LOG = Logger.getLogger(JdbcRoutes.class);

    @Override
    public void configure() {
        from("direct://get-generated-keys")
                .to("jdbc:camel-ds")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Object in = exchange.getIn().getHeader("CamelGeneratedKeysRows");
                        exchange.getIn().setBody(in);
                    }
                });

        from("direct://get-headers")
                .to("jdbc:camel-ds")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Object in = exchange.getIn().getHeaders();
                        exchange.getIn().setBody(in);
                    }
                });

        from("direct://headers-as-parameters")
                .to("jdbc:camel-ds?useHeadersAsParameters=true");

        from("timer://interval-polling?delay=2000&repeatCount=1").routeId("jdbc-poll").autoStartup(false)
                .setBody(constant("select * from camelsGenerated order by id desc"))
                .to("jdbc:camel-ds")
                .to("mock:interval-polling");

        from("direct://move-between-datasources")
                .setBody(constant("select * from camels"))
                .to("jdbc:camel-ds")
                .split(body())
                .setBody(simple("insert into camelsProcessed values('${body[ID]}','${body[SPECIES]}')"))
                .to("jdbc:camel-ds");
    }
}
