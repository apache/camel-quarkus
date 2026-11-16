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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mcp.server.McpToolCallContext;
import org.apache.camel.model.dataformat.JsonLibrary;

@ApplicationScoped
public class McpServerRoutes extends RouteBuilder {

    public static final String NOTES_DIR = "target/test-files";
    public static final String CLASHING_TOOL_ANSWER = "Camel AI tool called (declared with a from(\"ai-tool:...\") route)";
    // over 57 bytes, so that a MIME Base64 encoder would break the line
    public static final byte[] REPORT = "%PDF-1.7 latest report, long enough to span more than one MIME Base64 line"
            .getBytes(StandardCharsets.US_ASCII);

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

        // between tool-timeout and resource-timeout, like delayed_resource
        from("ai-tool:slow_tool?tags=conformance&description=Exceeds the tool timeout")
                .delay(4000)
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

        from("ai-tool:create_order?tags=conformance&description=Create an order"
                + "&argSchema=classpath:schema/create-order.json")
                .process(e -> {
                    Map<?, ?> customer = e.getMessage().getHeader("customer", Map.class);
                    List<?> items = e.getMessage().getHeader("items", List.class);
                    e.getMessage().setBody("order for customer " + customer.get("id") + " with " + items.size() + " item(s)");
                });

        from("ai-tool:order_status?tags=conformance&description=Get the status of an order"
                + "&outputSchema=classpath:schema/order-status.json")
                .setBody(constant("{\"orderId\":\"O-1\",\"status\":\"shipped\",\"items\":[\"BOOK\",\"PEN\"]}"));

        from("ai-tool:get_weather?tags=conformance&description=Get the weather"
                + "&outputParameter.temperature=number&outputParameter.temperature.required=true"
                + "&outputParameter.unit=string&outputParameter.unit.enum=celsius,fahrenheit")
                .setBody(constant("{\"temperature\":21.5,\"unit\":\"celsius\"}"));

        // the name is taken by the tool registered in QuarkusDefinedTools, so it is refused once started
        from("ai-tool:user_tool?tags=conformance"
                + "&description=Name already taken by an application tool, must be refused")
                .routeId("clashing-tool-route")
                .autoStartup(false)
                .setBody(constant("camel"));

        // the name is owned by the annotated tool in QuarkusDefinedTools; started with the application, so it is
        // refused while the MCP bridge starts, which must not fail the application startup
        from("ai-tool:clashing_tool?tags=conformance"
                + "&description=Name already taken by an annotated tool, must be refused")
                .routeId("annotated-clash-route")
                .setBody(constant(CLASHING_TOOL_ANSWER));

        from("ai-tool:whoami?tags=conformance&description=Return authenticated caller identity")
                .process(e -> {
                    Principal principal = e.getProperty(
                            McpToolCallContext.SECURITY_PRINCIPAL_PROPERTY, Principal.class);
                    String principalName = e.getProperty(
                            McpToolCallContext.SECURITY_PRINCIPAL_NAME_PROPERTY, String.class);
                    Collection<?> roles = e.getProperty(
                            McpToolCallContext.SECURITY_PRINCIPAL_ROLES_PROPERTY, Collection.class);
                    String roleNames = roles == null
                            ? ""
                            : roles.stream().map(String::valueOf).sorted().collect(Collectors.joining(","));
                    e.getMessage().setBody(
                            (principal != null ? principal.getName() : "no-principal")
                                    + "|" + (principalName != null ? principalName : "no-name")
                                    + "|" + roleNames);
                });

        from("ai-resource:app_config?resourceUri=camel:///config/app.json&tags=conformance"
                + "&description=Application configuration&mimeType=application/json&title=App config")
                .routeId("app-config-route")
                .setBody(constant("{\"env\":\"test\"}"));

        from("ai-resource:latest_report?resourceUri=camel:///reports/latest.pdf&tags=conformance"
                + "&description=Latest report&mimeType=application/pdf")
                .setBody(constant(REPORT));

        // an InputStream body, as returned by producers such as aws2-s3 getObject; the file is opened on every read
        from("ai-resource:notes?resourceUri=camel:///notes.txt&tags=conformance"
                + "&description=Notes read from a file&mimeType=text/plain")
                .process(e -> e.getMessage().setBody(Files.newInputStream(Paths.get(NOTES_DIR, "notes.txt"))));

        from("ai-resource:latest_order?resourceUri=camel:///orders/latest.json&tags=conformance"
                + "&description=Latest order&mimeType=application/json")
                .setBody(constant(new Order("A-1", 2)))
                .marshal().json(JsonLibrary.Jackson);

        from("ai-resource:fail_resource?resourceUri=camel:///fail&tags=conformance&description=Always fails")
                .process(e -> {
                    throw new IllegalStateException("secret internal detail");
                });

        from("ai-resource:slow_resource?resourceUri=camel:///slow&tags=conformance"
                + "&description=Exceeds the resource timeout")
                .delay(8000)
                .setBody(constant("done"));

        // between tool-timeout and resource-timeout, like slow_tool
        from("ai-resource:delayed_resource?resourceUri=camel:///delayed&tags=conformance"
                + "&description=Exceeds the tool timeout, not the resource timeout")
                .delay(3000)
                .setBody(constant("delayed"));

        from("ai-resource:hidden_resource?resourceUri=camel:///hidden"
                + "&description=Untagged resource, must not be exposed")
                .setBody(constant("hidden"));

        from("ai-resource:other_resource?resourceUri=camel:///other&tags=untrusted"
                + "&description=Not a selected tag, must not be exposed")
                .setBody(constant("other"));

        from("ai-resource:same_name?resourceUri=camel:///same/first&tags=conformance"
                + "&description=First resource named same_name")
                .setBody(constant("first"));

        from("ai-resource:same_name?resourceUri=camel:///same/second&tags=conformance"
                + "&description=Name already taken, must be refused")
                .setBody(constant("second"));

        // the uri is taken by the resource registered in QuarkusDefinedResources, so it is refused once started
        from("ai-resource:clashing_resource?resourceUri=camel:///user/clash&tags=conformance"
                + "&description=Uri already taken by an application resource, must be refused")
                .routeId("clashing-resource-route")
                .autoStartup(false)
                .setBody(constant("camel"));

        from("ai-resource:customer?uriTemplate=crm://customers/{id}&tags=conformance"
                + "&description=Customer details&mimeType=text/plain&title=Customer")
                .routeId("customer-resource-template-route")
                .process(e -> e.getMessage().setBody(
                        "customer " + e.getMessage().getHeader("CamelAiResourceTemplateVariable.id", String.class)));

        // The URI template is already owned by the application through ResourceTemplateManager.
        from("ai-resource:clashing_template?uriTemplate=crm://application/{id}&tags=conformance"
                + "&description=Template URI already taken by an application template")
                .routeId("clashing-resource-template-route")
                .autoStartup(false)
                .setBody(constant("camel"));

        from("ai-prompt:greeting?tags=conformance&description=Say hello&title=Greeting"
                + "&argument.name.description=Who to greet&argument.name.required=true")
                .routeId("greeting-prompt-route")
                .setBody(simple("Hello ${header.name}"));

        from("ai-prompt:map_prompt?tags=conformance&description=Return a content block")
                .routeId("map-prompt-route")
                .setBody(constant(List.of(Map.of(
                        "role", "assistant",
                        "content", Map.of("type", "text", "text", "Map content block")))));

        from("ai-prompt:hidden_prompt?description=Untagged prompt")
                .setBody(constant("hidden"));

        from("ai-prompt:other_prompt?tags=untrusted&description=Unselected prompt")
                .setBody(constant("other"));
    }
}
