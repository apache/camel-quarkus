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
package org.apache.camel.quarkus.component.platform.http.it;

import java.io.ByteArrayOutputStream;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.activation.DataHandler;

import org.apache.camel.Exchange;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.webhook.WebhookConfiguration;

public class PlatformHttpRouteBuilder extends RouteBuilder {
    @SuppressWarnings("unchecked")
    @Override
    public void configure() {
        rest()
                .get("/platform-http/rest-get")
                .route()
                .setBody(constant("GET: /rest-get"))
                .endRest()
                .post("/platform-http/rest-post")
                .consumes("text/plain").produces("text/plain")
                .route()
                .setBody(constant("POST: /rest-post"))
                .endRest();

        from("platform-http:/platform-http/hello?httpMethodRestrict=GET").setBody(simple("Hello ${header.name}"));
        from("platform-http:/platform-http/get-post?httpMethodRestrict=GET,POST").setBody(simple("Hello ${body}"));

        from("platform-http:/platform-http/multipart?httpMethodRestrict=POST")
                .to("log:multipart")
                .process(e -> {
                    final AttachmentMessage am = e.getMessage(AttachmentMessage.class);
                    final DataHandler src = am.getAttachment("bytes.bin");
                    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                        src.writeTo(out);
                        e.getMessage().setBody(out.toByteArray());
                    }
                });

        from("platform-http:/platform-http/form-urlencoded?httpMethodRestrict=POST")
                .to("log:form-urlencoded")
                .setBody(e -> ((Map<String, Object>) e.getMessage().getBody(Map.class)).entrySet().stream()
                        .map(en -> en.getKey() + "=" + en.getValue().toString().toUpperCase(Locale.US))
                        .collect(Collectors.joining("\n")));

        from("platform-http:/platform-http/header-filter-strategy?httpMethodRestrict=GET&headerFilterStrategy=#TestHeaderFilterStrategy")
                .to("log:header-filter-strategy")
                .setBody(simple("k1=${header.k1}\nk2=${header.k2}"));

        from("platform-http:/platform-http/multi-value-params?httpMethodRestrict=GET")
                .to("log:multi-value-params")
                .setBody(simple("k1=${header.k1}"));

        from("platform-http:/platform-http/encoding?httpMethodRestrict=POST")
                .to("log:encoding")
                .setBody(e -> e.getMessage().getBody(String.class))
                .setHeader("Content-Type").constant("text/plain ; charset=UTF-8");

        from("platform-http:/platform-http/response-code-299?httpMethodRestrict=GET")
                .to("log:response-code")
                .setHeader(Exchange.HTTP_RESPONSE_CODE).constant(299);

        from("platform-http:/platform-http/consumes?httpMethodRestrict=POST&consumes=text/plain")
                .setBody(simple("Hello ${body}"));

        from("platform-http:/platform-http/produces?httpMethodRestrict=POST&produces=text/plain")
                .setBody(simple("Hello ${body}"));

        /* 204 tests */
        from("platform-http:/platform-http/null-body")
                .setBody(constant(null));
        from("platform-http:/platform-http/empty-string-body")
                .setBody().constant("");
        from("platform-http:/platform-http/some-string")
                .setBody().constant("No Content");
        from("platform-http:/platform-http/empty-string-200")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
                .setBody().constant("");

        /* Path parameters */
        rest()
                .get("/platform-http/hello-by-name/{name}")
                .produces("text/plain")
                .route()
                .setBody(e -> "Hello " + e.getIn().getHeader("name", String.class))
                .endRest();

        // Webhook tests
        from("platform-http:/platform-http/webhookpath")
                .setBody(constant(WebhookConfiguration.computeDefaultPath("webhook-delegate://test")));

        from("webhook:webhook-delegate://test")
                .transform(body().prepend("Hello "));
    }
}
