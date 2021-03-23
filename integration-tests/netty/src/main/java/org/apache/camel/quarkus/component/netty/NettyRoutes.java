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
package org.apache.camel.quarkus.component.netty;

import org.apache.camel.builder.RouteBuilder;

public class NettyRoutes extends RouteBuilder {

    @Override
    public void configure() {
        from("netty:tcp://localhost:{{camel.netty.test-tcp-port}}?textline=true&sync=true")
                .transform().simple("Hello ${body} TCP");

        from("netty:udp://localhost:{{camel.netty.test-udp-port}}?sync=true")
                .transform().simple("Hello ${body} UDP");

        from("netty:tcp://localhost:{{camel.netty.test-codec-tcp-port}}?disconnect=true&sync=false&allowDefaultCodec=false&decoders=#tcpNullDelimitedHandler,#bytesDecoder&encoders=#bytesEncoder")
                .convertBodyTo(String.class)
                .transform().simple("Hello ${body} TCP")
                .to("seda:custom-tcp-codec");

        from("netty:udp://localhost:{{camel.netty.test-codec-udp-port}}?udpByteArrayCodec=true&sync=false")
                .transform().simple("Hello ${body} UDP")
                .to("seda:custom-udp-codec");
    }
}
