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
package org.apache.camel.quarkus.component.ai.tool.it;

import org.apache.camel.builder.RouteBuilder;

public class AiToolRoutes extends RouteBuilder {
    @Override
    public void configure() {
        from("ai-tool:getWeather?tags=weather"
                + "&description=Get the current weather for a city"
                + "&parameter.city=string"
                + "&parameter.city.description=The city name"
                + "&parameter.city.required=true"
                + "&parameter.unit=string"
                + "&parameter.unit.enum=celsius,fahrenheit")
                .setBody(simple("Weather in ${header.city}: sunny, 25 ${header.unit}"));

        from("ai-tool:calculate?tags=math"
                + "&description=Perform a calculation"
                + "&parameter.a=integer"
                + "&parameter.a.required=true"
                + "&parameter.b=integer"
                + "&parameter.b.required=true"
                + "&parameter.operation=string"
                + "&parameter.operation.required=true"
                + "&parameter.operation.enum=add,subtract,multiply")
                .choice()
                .when(simple("${header.operation} == 'add'"))
                .process(e -> {
                    int a = e.getMessage().getHeader("a", Integer.class);
                    int b = e.getMessage().getHeader("b", Integer.class);
                    e.getMessage().setBody(String.valueOf(a + b));
                })
                .when(simple("${header.operation} == 'subtract'"))
                .process(e -> {
                    int a = e.getMessage().getHeader("a", Integer.class);
                    int b = e.getMessage().getHeader("b", Integer.class);
                    e.getMessage().setBody(String.valueOf(a - b));
                })
                .when(simple("${header.operation} == 'multiply'"))
                .process(e -> {
                    int a = e.getMessage().getHeader("a", Integer.class);
                    int b = e.getMessage().getHeader("b", Integer.class);
                    e.getMessage().setBody(String.valueOf(a * b));
                })
                .otherwise()
                .setBody(constant("Unknown operation"));

        from("ai-tool:greet?"
                + "description=Greet a user by name"
                + "&parameter.name=string"
                + "&parameter.name.required=true")
                .setBody(simple("Hello, ${header.name}!"));

        from("ai-tool:failingTool?tags=test"
                + "&description=A tool that always fails")
                .throwException(new IllegalStateException("Intentional failure"));

        // CamelOverride is declared, but argument names starting with "Camel" must never become headers;
        // "extra" is not declared, so it must be dropped
        from("ai-tool:echoHeaders?tags=test"
                + "&description=Echo the received headers"
                + "&parameter.name=string"
                + "&parameter.CamelOverride=string")
                .setBody(simple("name=${header.name}, camel=${header.CamelOverride}, extra=${header.extra}"));

        // No description, several tags; the route sets no body
        from("ai-tool:typedTool?tags=weather,math")
                .log("typedTool called");

        from("ai-tool:searchInline?tags=search"
                + "&description=Search with an inline schema"
                + "&argSchema=RAW({\"type\":\"object\",\"properties\":{\"query\":{\"type\":\"string\"}},"
                + "\"required\":[\"query\"]})")
                .setBody(simple("Results for ${header.query}"));

        from("ai-tool:searchFromResource?tags=search"
                + "&description=Search with a schema from the classpath"
                + "&argSchema=classpath:ai-tool/search-schema.json")
                .setBody(simple("Results for ${header.query}"));
    }
}
