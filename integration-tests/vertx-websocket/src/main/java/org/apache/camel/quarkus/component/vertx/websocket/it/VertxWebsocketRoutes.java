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
package org.apache.camel.quarkus.component.vertx.websocket.it;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.vertx.websocket.VertxWebsocketConstants;

public class VertxWebsocketRoutes extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("vertx-websocket:/echo")
                .setBody(simple("Hello ${body}"))
                .to("vertx-websocket:/echo");

        from("vertx-websocket:/")
                .choice().when().simple("${body} == 'ping'")
                .setBody().header(VertxWebsocketConstants.CONNECTION_KEY)
                .endChoice()
                .otherwise()
                .setBody().simple("Hello ${body}")
                .end()
                .to("vertx-websocket:/");

        from("vertx-websocket:redundant.host:9999/test/default/host/port/applied")
                .setBody(simple("Hello ${body}"))
                .to("vertx-websocket:/test/default/host/port/applied");

        from("direct:sendMessage")
                .to("vertx-websocket:/test");

        from("direct:produceToExternalEndpoint")
                .toD("vertx-websocket:${header.host}:${header.port}/managed/by/quarkus/websockets");

        from("vertx-websocket:/client/consumer")
                .to("vertx-websocket:/client/consumer?sendToAll=true");

        from("vertx-websocket:/client/consumer?consumeAsClient=true").routeId("consumeAsClientRoute").autoStartup(false)
                .setBody().simple("Hello ${body}")
                .to("seda:consumeAsClientResult");

        from("vertx-websocket:/parameterized/path/{paramA}/{paramB}")
                .setBody().simple("${header.paramA} ${header.paramB}")
                .to("seda:parameterizedPathResult");

        from("vertx-websocket:/query/params")
                .setBody().simple("${header.paramA} ${header.paramB}")
                .to("seda:queryParamsResult");

        from("vertx-websocket:/events?fireWebSocketConnectionEvents=true")
                .setBody().header(VertxWebsocketConstants.EVENT)
                .to("seda:eventsResult");
    }
}
