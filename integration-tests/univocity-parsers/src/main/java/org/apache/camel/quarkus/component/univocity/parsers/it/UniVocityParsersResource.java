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
package org.apache.camel.quarkus.component.univocity.parsers.it;

import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;
import org.jboss.logging.Logger;

@Path("/univocity-parsers")
@ApplicationScoped
public class UniVocityParsersResource {

    private static final Logger LOG = Logger.getLogger(UniVocityParsersResource.class);

    @Inject
    ProducerTemplate template;

    @Path("/marshal/{dataformat : (csv|fixed-width|tsv)}/{test}")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String marshal(@PathParam("dataformat") String df, @PathParam("test") String testName,
            List<Map<String, String>> objectsToMarshal) {
        LOG.debugf("Calling marshal with (dataformat = %s, test = %s and content =\n%s", df, testName, objectsToMarshal);
        if (objectsToMarshal.size() == 1) {
            // Force single line marshal to complete test coverage
            return template.requestBody("direct:" + df + "-marshal-" + testName, objectsToMarshal.get(0), String.class);
        }
        return template.requestBody("direct:" + df + "-marshal-" + testName, objectsToMarshal, String.class);
    }

    @Path("/unmarshal/{dataformat : (csv|fixed-width|tsv)}/{test}")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public List<?> unmarshal(@PathParam("dataformat") String df, @PathParam("test") String testName, String content) {
        LOG.debugf("Calling unmarshal with (dataformat = %s, test = %s and content =\n%s", df, testName, content);
        return template.requestBody("direct:" + df + "-unmarshal-" + testName, content, List.class);
    }

}
