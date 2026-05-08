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
package org.apache.camel.quarkus.component.jsoup.it;

import org.apache.camel.builder.RouteBuilder;
import org.jsoup.nodes.Document;

public class JsoupRouteBuilder extends RouteBuilder {
    @Override
    public void configure() {
        // Test htmlClean() function
        from("direct:clean")
                .transform().simple("${htmlClean()}");

        // Test htmlDecode() function
        from("direct:decode")
                .transform().simple("${htmlDecode()}");

        // Test htmlParse() function - extract title
        from("direct:parse-title")
                .transform().simple("${htmlParse()}")
                .process(exchange -> {
                    Document doc = exchange.getMessage().getBody(Document.class);
                    exchange.getMessage().setBody(doc.title());
                });

        // Test htmlParse() with CSS selection
        from("direct:select-css")
                .transform().simple("${htmlParse()}")
                .process(exchange -> {
                    Document doc = exchange.getMessage().getBody(Document.class);
                    String text = doc.select("p").text();
                    exchange.getMessage().setBody(text);
                });
    }
}
