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
package org.apache.camel.quarkus.component.netty.tcp;

import org.apache.camel.builder.RouteBuilder;

public class NettyTcpRoutes extends RouteBuilder {

    @Override
    public void configure() {
        from("netty:tcp://localhost:{{camel.netty.test-tcp-port}}?textline=true&sync=true")
                .transform().simple("Hello ${body} TCP");

        from("netty:tcp://localhost:{{camel.netty.test-bytebuf-tcp-port}}?sync=true")
                .transform().simple("Hello ${body} TCP");

        from("netty:tcp://localhost:{{camel.netty.test-codec-tcp-port}}?disconnect=true&sync=false&allowDefaultCodec=false&decoders=#tcpNullDelimitedHandler,#bytesDecoder&encoders=#bytesEncoder")
                .convertBodyTo(String.class)
                .transform().simple("Hello ${body} TCP")
                .to("seda:custom-tcp-codec");

        from("netty:tcp://localhost:{{camel.netty.test-ssl-tcp-port}}?textline=true&sync=true&ssl=true&sslContextParameters=#sslContextParameters")
                .transform().simple("Hello ${body} TCP SSL");

        from("netty:tcp://localhost:{{camel.netty.test-server-initializer-tcp-port}}?sync=true&serverInitializerFactory=#serverInitializerFactory")
                .process(exchange -> {
                    // Noop
                });

        from("netty:tcp://localhost:{{camel.netty.test-worker-group-tcp-port}}?textline=true&sync=true&bossGroup=#bossGroup&workerGroup=#workerGroup&usingExecutorService=false")
                .transform().simple("Hello ${body} TCP Custom Worker Group");

        from("seda:correlationManagerTcp")
                .to("netty:tcp://localhost:{{camel.netty.test-correlation-manager-tcp-port}}?textline=true&sync=true&producerPoolEnabled=false&correlationManager=#correlationManager")
                .to("mock:correlationManagerTcp");

        from("netty:tcp://localhost:{{camel.netty.test-correlation-manager-tcp-port}}?textline=true&sync=true")
                .transform(body().prepend("Bye "));

        from("netty:tcp://localhost:{{camel.netty.test-serialization-tcp-port}}?sync=true&transferExchange=true&encoders=#tcpObjectEncoder&decoders=#tcpObjectDecoder")
                .to("mock:tcpObjectResult");
    }
}
