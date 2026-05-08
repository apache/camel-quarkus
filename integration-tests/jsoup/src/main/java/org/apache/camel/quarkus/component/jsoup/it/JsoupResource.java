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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;

@Path("/jsoup")
@ApplicationScoped
public class JsoupResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/clean")
    @POST
    @Consumes(MediaType.TEXT_HTML)
    @Produces(MediaType.TEXT_HTML)
    public String clean(String html) {
        return producerTemplate.requestBody("direct:clean", html, String.class);
    }

    @Path("/decode")
    @POST
    @Consumes(MediaType.TEXT_HTML)
    @Produces(MediaType.TEXT_PLAIN)
    public String decode(String html) {
        return producerTemplate.requestBody("direct:decode", html, String.class);
    }

    @Path("/parse-title")
    @POST
    @Consumes(MediaType.TEXT_HTML)
    @Produces(MediaType.TEXT_PLAIN)
    public String parseTitle(String html) {
        return producerTemplate.requestBody("direct:parse-title", html, String.class);
    }

    @Path("/select-css")
    @POST
    @Consumes(MediaType.TEXT_HTML)
    @Produces(MediaType.TEXT_PLAIN)
    public String selectCss(String html) {
        return producerTemplate.requestBody("direct:select-css", html, String.class);
    }

    @Path("/clean-yaml")
    @POST
    @Consumes(MediaType.TEXT_HTML)
    @Produces(MediaType.TEXT_HTML)
    public String cleanYaml(String html) {
        return producerTemplate.requestBody("direct:clean-yaml", html, String.class);
    }

    @Path("/decode-yaml")
    @POST
    @Consumes(MediaType.TEXT_HTML)
    @Produces(MediaType.TEXT_PLAIN)
    public String decodeYaml(String html) {
        return producerTemplate.requestBody("direct:decode-yaml", html, String.class);
    }

    @Path("/parse-yaml")
    @POST
    @Consumes(MediaType.TEXT_HTML)
    @Produces(MediaType.TEXT_HTML)
    public String parseYaml(String html) {
        org.jsoup.nodes.Document doc = producerTemplate.requestBody("direct:parse-yaml", html, org.jsoup.nodes.Document.class);
        return doc != null ? doc.outerHtml() : "";
    }
}
