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
package org.apache.camel.quarkus.component.jolt.it;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.jolt.JoltConstants;
import org.jboss.logging.Logger;

@Path("/jolt")
@ApplicationScoped
public class JoltResource {

    private static final Logger LOG = Logger.getLogger(JoltResource.class);

    @Inject
    ProducerTemplate template;

    @Path("/defaultr")
    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String defaultr(String value) {
        LOG.infof("Calling defaultr with %s", value);
        Map<String, String> inbody = new HashMap<>();
        inbody.put("key", value);
        Map<?, ?> outBody = template.requestBody("jolt:defaultr.json?transformDsl=Defaultr", inbody, Map.class);
        Map<?, ?> outObject = (Map<?, ?>) outBody.get("object");
        return String.format("%s-%s-%s-%s-%s-%s+%s", outBody.get("string"), outBody.get("null"), outBody.get("array"),
                outBody.get("floating"), outBody.get("integer"), outObject.get("boolean"), outBody.get("key"));
    }

    @Path("/removr")
    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String removr(String value) {
        LOG.infof("Calling removr with %s", value);
        Map<String, String> inBody = new HashMap<>();
        inBody.put("keepMe", "Kept");
        inBody.put("key", value);
        inBody.put("removeMe", "This should be gone");
        Map<?, ?> outBody = template.requestBody("jolt:removr.json?transformDsl=Removr", inBody, Map.class);
        return String.format("%s-%s+%s", outBody.size(), outBody.get("keepMe"), outBody.get("key"));
    }

    @Path("/sample")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String sample(String inputJson) {
        LOG.infof("Calling sample with %s", inputJson);
        Map<String, String> context = new HashMap<>();
        context.put("contextB", "bb");
        String joltEndpointUri = "jolt:sample-spec.json?inputType=JsonString&outputType=JsonString&allowTemplateFromHeader=true";
        return template.requestBodyAndHeader(joltEndpointUri, inputJson, JoltConstants.JOLT_CONTEXT, context, String.class);
    }

    @Path("/function")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String function(String inputJson) {
        LOG.infof("Calling function with %s", inputJson);
        String joltEndpointUri = "jolt:function-spec.json?inputType=JsonString&outputType=JsonString";
        return template.requestBody(joltEndpointUri, inputJson, String.class);
    }
}
