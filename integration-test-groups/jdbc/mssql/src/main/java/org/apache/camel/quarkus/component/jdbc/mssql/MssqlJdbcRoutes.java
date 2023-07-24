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
package org.apache.camel.quarkus.component.jdbc.mssql;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class MssqlJdbcRoutes extends RouteBuilder {
    private String dbKind = "mssql";

    @Override
    public void configure() {
        from("direct://get-generated-keys-" + dbKind)
                .to("jdbc:" + dbKind)
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        System.out.println(exchange.getIn().getHeaders());
                        Object in = exchange.getIn().getHeader("CamelGeneratedKeysRows");
                        exchange.getIn().setBody(in);
                    }
                });

        from("direct://get-headers-" + dbKind)
                .to("jdbc:" + dbKind)
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Object in = exchange.getIn().getHeaders();
                        exchange.getIn().setBody(in);
                    }
                });

        from("direct://headers-as-parameters-" + dbKind)
                .to("jdbc:" + dbKind + "?useHeadersAsParameters=true");

        from("timer://interval-polling-" + dbKind + "?delay=2000&repeatCount=1").routeId("jdbc-poll-" + dbKind)
                .autoStartup(false)
                .setBody(constant("select * from camelsGenerated order by id desc"))
                .to("jdbc:" + dbKind)
                .to("mock:interval-polling-" + dbKind);

        String species = "species";

        from("direct://move-between-datasources-" + dbKind)
                .setBody(constant("select * from camels"))
                .to("jdbc:" + dbKind)
                .split(body())
                .setBody(simple("insert into camelsProcessed (species) values('${body[" + species + "]}')"))
                .to("jdbc:" + dbKind);
    }
}
