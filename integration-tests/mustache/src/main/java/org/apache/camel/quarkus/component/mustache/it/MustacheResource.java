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
package org.apache.camel.quarkus.component.mustache.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mustache.MustacheConstants;
import org.jboss.logging.Logger;

@Path("/mustache")
@ApplicationScoped
public class MustacheResource {

    private static final Logger LOG = Logger.getLogger(MustacheResource.class);

    @Inject
    ProducerTemplate template;

    @Path("/templateFromClassPathResource")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String templateFromClassPathResource(String message) {
        LOG.infof("Calling templateFromClassPathResource with %s", message);
        return template.requestBodyAndHeader("mustache://template/simple.mustache", message, "header", "value", String.class);
    }

    @Path("/templateFromHeader")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String templateFromHeader(String message) {
        LOG.infof("Calling templateFromHeader with %s", message);
        return template.requestBodyAndHeader("mustache://template/simple.mustache?allowTemplateFromHeader=true", message,
                MustacheConstants.MUSTACHE_TEMPLATE,
                "Body='{{body}}'", String.class);
    }

    @Path("/templateUriFromHeader")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String templateUriFromHeader(String message) {
        LOG.infof("Calling templateUriFromHeader with %s", message);
        return template.requestBodyAndHeader("mustache://template/simple.mustache?allowTemplateFromHeader=true", message,
                MustacheConstants.MUSTACHE_RESOURCE_URI,
                "/template/another.mustache", String.class);
    }

    @Path("/templateWithInheritance")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String templateWithInheritance() {
        LOG.infof("Calling templateWithInheritance");
        return template.requestBody("mustache://template/child.mustache", null, String.class);
    }

    @Path("/templateWithPartials")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String templateWithPartials() {
        LOG.infof("Calling templateWithPartials");
        return template.requestBody("mustache://template/includer.mustache", null, String.class);
    }

    @Path("/templateFromRegistry")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String templateFromRegistry(String message) {
        LOG.infof("Calling templateFromRegistry with %s", message);
        return template.requestBody("mustache://ref:templateFromRegistry", message, String.class);
    }
}
