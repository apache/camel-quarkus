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
package org.apache.camel.quarkus.component.qute.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.qute.QuteConstants;

@Path("/qute")
@ApplicationScoped
public class QuteResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/template")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String getTemplateFromHeader(String content) throws Exception {
        return producerTemplate.requestBodyAndHeader("qute:hello?allowTemplateFromHeader=true", "World",
                QuteConstants.QUTE_TEMPLATE, content, String.class);
    }

    @Path("/template/{name}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getTemplateFromPath(@PathParam("name") String name) throws Exception {
        return producerTemplate.requestBody("direct:test", name, String.class);
    }

    @Path("/template/invalid/path")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getInvalidTemplatePath() throws Exception {
        try {
            producerTemplate.requestBody("qute:invalid-path", "test", String.class);
            return null;
        } catch (CamelExecutionException e) {
            return e.getCause().getMessage();
        }
    }
}
