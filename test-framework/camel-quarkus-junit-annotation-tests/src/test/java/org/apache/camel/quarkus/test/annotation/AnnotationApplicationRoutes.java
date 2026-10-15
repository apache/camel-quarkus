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
package org.apache.camel.quarkus.test.annotation;

import org.apache.camel.builder.RouteBuilder;

/**
 * An application route, discovered like any route in the application under test.
 */
public class AnnotationApplicationRoutes extends RouteBuilder {
    public static final String ROUTE_ID = "annotation-app";
    public static final String NESTED_ROUTE_ID = "annotation-app-nested";

    @Override
    public void configure() {
        from("direct:annotationApp")
                .routeId(ROUTE_ID)
                .transform().simple("Hello ${body}")
                .to("log:annotationApp")
                .to("mock:annotationApp");

        from("direct:annotationAppNested")
                .routeId(NESTED_ROUTE_ID)
                .filter(simple("${body} != 'skip'"))
                .to("mock:annotationAppNested")
                .end()
                .to("log:annotationAppNested");
    }
}
