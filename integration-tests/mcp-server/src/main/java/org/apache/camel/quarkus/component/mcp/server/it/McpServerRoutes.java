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
package org.apache.camel.quarkus.component.mcp.server.it;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class McpServerRoutes extends RouteBuilder {

    @Override
    public void configure() {
        from("ai-tool:say_hello?tags=conformance&description=Say hello"
                + "&parameter.name=string&parameter.name.description=Who to greet&parameter.name.required=true")
                .routeId("say-hello-route")
                .setBody(simple("Hello ${header.name}"));

        from("ai-tool:fail_tool?tags=conformance&description=Always fails")
                .process(e -> {
                    throw new IllegalStateException("secret internal detail");
                });

        from("ai-tool:slow_tool?tags=conformance&description=Exceeds the tool timeout")
                .delay(6000)
                .setBody(constant("done"));

        from("ai-tool:hidden_tool?description=Untagged tool, must not be exposed")
                .setBody(constant("hidden"));

        from("ai-tool:other_tool?tags=untrusted&description=Not a selected tag, must not be exposed")
                .setBody(constant("other"));
    }
}
