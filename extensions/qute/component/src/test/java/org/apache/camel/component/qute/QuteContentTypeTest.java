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
package org.apache.camel.component.qute;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class QuteContentTypeTest extends QuteTestBase {
    private static final String MESSAGE = "<h1>Hello World!</h1>";
    private static final String MESSAGE_ENCODED = "&lt;h1&gt;Hello World!&lt;/h1&gt;";

    @Test
    void customTemplateContentType() throws IOException {
        Map<String, Object> headers = new HashMap<>();

        try (InputStream stream = getClass().getResourceAsStream("hello.html")) {
            if (stream == null) {
                throw new IllegalArgumentException("hello.html not found");
            }

            String content = context.getTypeConverter().convertTo(String.class, stream);

            headers.put(QuteConstants.QUTE_TEMPLATE, content);
            headers.put(QuteConstants.QUTE_TEMPLATE_CONTENT_TYPE, "text/html");

            String result = template.requestBodyAndHeaders("direct:start", MESSAGE, headers, String.class);
            assertTrue(result.contains(MESSAGE_ENCODED));
        }
    }

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() {
                from("direct:start")
                        .to("qute:dynamic?allowTemplateFromHeader=true");
            }
        };
    }
}
