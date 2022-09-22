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
package org.apache.camel.quarkus.component.rest.openapi.it;

import org.apache.camel.builder.RouteBuilder;

public class RestOpenApiRoutes extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("direct:start-web-json")
                .toD("rest-openapi:#list?specificationUri=RAW(http://localhost:${header.test-port}/q/openapi?format=JSON)");

        from("direct:start-web-yaml")
                .toD("rest-openapi:#list?specificationUri=RAW(http://localhost:${header.test-port}/q/openapi?format=YAML)");

        from("direct:start-file")
                .toD("rest-openapi:#list?specificationUri=file:target/openapi.json&host=RAW(http://localhost:${header.test-port})");

        from("direct:start-bean")
                .toD("rest-openapi:#list?specificationUri=bean:openapi.getOpenApiJson&host=RAW(http://localhost:${header.test-port})");

        from("direct:start-classpath")
                .toD("rest-openapi:#list?specificationUri=classpath:openapi.json&host=RAW(http://localhost:${header.test-port})");
    }
}
