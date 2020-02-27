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
package org.apache.camel.quarkus.component.http.it;

import org.apache.camel.builder.RouteBuilder;

public class HttpRoute extends RouteBuilder {
    @Override
    public void configure() {
        from("netty-http:http://0.0.0.0:{{camel.netty-http.test-port}}/test/server/hello")
                .transform().constant("Netty Hello World");

        from("direct:ahcWsIn")
                .toD("ahc-ws:localhost:${header.test-port}/ahc-ws/greeting");
    }
}
