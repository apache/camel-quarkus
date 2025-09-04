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
package org.apache.camel.quarkus.component.groovy.xml.it;

import java.util.List;

import groovy.util.Node;
import groovy.xml.XmlParser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;

@Path("/groovy-xml")
@ApplicationScoped
public class GroovyXmlResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/unmarshal")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public List<String> unmarshal(String message) throws Exception {
        final Node node = producerTemplate.requestBody("direct:unmarshal", message, Node.class);

        //extract ids from the children
        return node.children().stream().map(ch -> ((Node) ch).attribute("id")).toList();
    }

    @Path("/marshal")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String marshal(String message) throws Exception {

        final XmlParser parser = new XmlParser();
        parser.setTrimWhitespace(false);
        final Node node = parser.parseText(message);

        return producerTemplate.requestBody("direct:marshal", node, String.class);
    }

}
