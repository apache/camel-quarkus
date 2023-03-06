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
package org.apache.camel.quarkus.component.language.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;

@Path("/language")
@ApplicationScoped
public class LanguageResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/route/{route}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String route(String body, @PathParam("route") String route) {
        return producerTemplate.requestBody("direct:" + route, body, String.class);
    }

    @Path("/route/languageFileScript")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String routeFileScript(String body) {
        return consumerTemplate.receiveBody("direct:languageFileOutput", 5000, String.class);
    }

    @Path("/route/languageSimpleHttp")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String routeSimpleHttp(String body, @QueryParam("baseUrl") String baseUrl) {
        return producerTemplate.requestBody("language:simple:resource:" + baseUrl + "/simple", body, String.class);
    }

    @Path("/route/languageSimpleContentCache")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String routeSimpleContentCache(String body, @QueryParam("baseUrl") String baseUrl,
            @QueryParam("contentCache") String contentCache) {
        String option = "?contentCache=" + contentCache;
        String url = "language:simple:resource:" + baseUrl + "/simpleContentCache" + option;
        return producerTemplate.requestBody(url, body, String.class);
    }
}
