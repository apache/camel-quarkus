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

import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class McpServerRoutes extends RouteBuilder {

    public static final String CLASHING_TOOL_ANSWER = "Camel AI tool called (declared with a from(\"ai-tool:...\") route)";

    @Override
    public void configure() {
        from("ai-tool:say_hello?tags=conformance&description=Say hello"
                + "&parameter.name=string&parameter.name.description=Who to greet&parameter.name.required=true")
                .routeId("say-hello-route")
                .setBody(simple("Hello ${header.name}"));

        from("ai-tool:missing_required_arg_tool?tags=conformance&description=Description"
                + "&parameter.name=string&parameter.name.description=RequiredParam&parameter.name.required=true")
                .routeId("missing-required-arg-route")
                .setBody(simple("The required tool argument 'name' was set: ${header.name}"));

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

        from("ai-tool:annotated_tool?tags=conformance&description=Tool with annotation hints"
                + "&title=Annotated tool"
                + "&readOnlyHint=true"
                + "&idempotentHint=true")
                .setBody(constant("annotated"));

        from("ai-tool:unannotated_tool?tags=conformance&description=Tool without annotation hints")
                .setBody(constant("unannotated"));

        // the name is owned by the annotated tool in QuarkusDefinedTools; started with the application, so it is
        // refused while the MCP bridge starts, which must not fail the application startup
        from("ai-tool:clashing_tool?tags=conformance"
                + "&description=Name already taken by an annotated tool, must be refused")
                .routeId("annotated-clash-route")
                .setBody(constant(CLASHING_TOOL_ANSWER));

        from("ai-tool:create_order?tags=conformance&description=Create an order"
                + "&argSchema=classpath:schema/create-order.json")
                .process(e -> {
                    Map<?, ?> customer = e.getMessage().getHeader("customer", Map.class);
                    List<?> items = e.getMessage().getHeader("items", List.class);
                    e.getMessage().setBody("order for customer " + customer.get("id") + " with " + items.size() + " item(s)");
                });
    }
}
