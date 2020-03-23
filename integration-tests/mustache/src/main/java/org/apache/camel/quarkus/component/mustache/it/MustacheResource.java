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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mustache.MustacheConstants;
import org.jboss.logging.Logger;

@Path("/mustache")
@ApplicationScoped
public class MustacheResource {

    private static final Logger LOG = Logger.getLogger(MustacheResource.class);

    @Inject
    ProducerTemplate template;

    @Path("/applyMustacheTemplateFromClassPathResource")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String applyMustacheTemplateFromClassPathResource(String message) {
        LOG.infof("Calling applyMustacheTemplateFromClassPathResource with %s", message);
        return template.requestBodyAndHeader("mustache://template/simple.mustache", message, "header", "value", String.class);
    }

    @Path("/applyMustacheTemplateFromHeader")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String applyMustacheTemplateFromHeader(String message) {
        LOG.infof("Calling applyMustacheTemplateFromHeader with %s", message);
        return template.requestBodyAndHeader("mustache://template/simple.mustache", message,
                MustacheConstants.MUSTACHE_TEMPLATE,
                "Body='{{body}}'", String.class);
    }

    @Path("/applyMustacheTemplateUriFromHeader")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String applyMustacheTemplateUriFromHeader(String message) {
        LOG.infof("Calling applyMustacheTemplateUriFromHeader with %s", message);
        return template.requestBodyAndHeader("mustache://template/simple.mustache", message,
                MustacheConstants.MUSTACHE_RESOURCE_URI,
                "/template/another.mustache", String.class);
    }

    @Path("/applyMustacheTemplateWithInheritance")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String applyMustacheTemplateWithInheritance() {
        LOG.infof("Calling applyMustacheTemplateWithInheritance");
        return template.requestBody("mustache://template/child.mustache", null, String.class);
    }

    @Path("/applyMustacheTemplateWithPartials")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String applyMustacheTemplateWithPartials() {
        LOG.infof("Calling applyMustacheTemplateWithPartials");
        return template.requestBody("mustache://template/includer.mustache", null, String.class);
    }

    @Path("/applyMustacheTemplateFromRegistry")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String applyMustacheTemplateFromRegistry(String message) {
        LOG.infof("Calling applyMustacheTemplateFromRegistry with %s", message);
        return template.requestBody("mustache://ref:templateFromRegistry", message, String.class);
    }
}
