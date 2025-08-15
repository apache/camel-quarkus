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
package org.apache.camel.quarkus.component.langchain4j.tools.it;

import com.fasterxml.jackson.databind.node.IntNode;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;

public class Langchain4jToolsRoutes extends RouteBuilder {
    @Override
    public void configure() {
        from("direct:start")
                .to("langchain4j-tools:userInfo?tags=users");

        from("langchain4j-tools:userInfo?tags=users&description=Query database by user ID&parameter.user_id=integer")
                .process(this::convertUserIdToIntIfRequired)
                .to("sql:SELECT first_name, last_name FROM users WHERE id = :#user_id");
    }

    private void convertUserIdToIntIfRequired(Exchange exchange) {
        // Unfortunately, we sometimes need to cast header values to the correct type
        // TODO: Investigate removing this with Camel >= 4.13
        Message message = exchange.getMessage();
        Object header = message.getHeader("user_id");
        if (header instanceof IntNode) {
            message.setHeader("user_id", ((IntNode) header).asInt());
        }
    }
}
