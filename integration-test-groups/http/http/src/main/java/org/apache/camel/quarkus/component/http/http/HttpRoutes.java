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
package org.apache.camel.quarkus.component.http.http;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;

public class HttpRoutes extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        from("direct:http-https").to(
                "https://localhost:{{quarkus.http.test-ssl-port}}/service/common/https?sslContextParameters=#sslContextParameters");

        from("direct:httpOperationFailedException")
                .onException(HttpOperationFailedException.class)
                .handled(true)
                .setBody().constant("Handled HttpOperationFailedException")
                .to("seda:dlq")
                .end()
                .to("http://localhost:{{quarkus.http.test-port}}/service/common/error");
    }
}
