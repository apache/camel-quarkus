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
package org.apache.camel.quarkus.component.sap.netweaver.it;

import java.io.InputStream;

import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

public class SapNetweaverRoutes extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("netty-http:0.0.0.0:{{camel.netty.test-port}}/sap/api/json?matchOnUriPrefix=true")
                .process(createProcessor("/flight-data.json"));

        from("netty-http:0.0.0.0:{{camel.netty.test-port}}/sap/api/xml?matchOnUriPrefix=true")
                .process(createProcessor("/flight-data.xml"));
    }

    private Processor createProcessor(String path) {
        return exchange -> {
            InputStream resource = SapNetweaverRoutes.class.getResourceAsStream(path);
            exchange.getMessage().setBody(resource);
        };
    }
}
