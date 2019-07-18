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
package org.apache.camel.quarkus.component.infinispan;

import java.nio.charset.StandardCharsets;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.infinispan.InfinispanConstants;
import org.apache.camel.component.infinispan.InfinispanOperation;

public class CamelRoute extends RouteBuilder {

    @Override
    public void configure() {

        from("netty4-http:http://0.0.0.0:8999/put")
                .convertBodyTo(byte[].class)
                .to("log:cache?showAll=true")
                .setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.PUT)
                .setHeader(InfinispanConstants.KEY).constant("the-key".getBytes(StandardCharsets.UTF_8))
                .setHeader(InfinispanConstants.VALUE).body()
                .to("infinispan:default?hosts=localhost:11232");

        from("netty4-http:http://0.0.0.0:8999/get")
                .setHeader(InfinispanConstants.OPERATION)
                .constant(InfinispanOperation.GET)
                .setHeader(InfinispanConstants.KEY)
                .constant("the-key".getBytes(StandardCharsets.UTF_8))
                .to("infinispan:default?hosts=localhost:11232")
                .to("log:cache?showAll=true");
    }

}
