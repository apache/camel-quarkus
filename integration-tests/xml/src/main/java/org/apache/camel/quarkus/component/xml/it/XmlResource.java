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
package org.apache.camel.quarkus.component.xml.it;

import java.util.StringJoiner;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.jboss.logging.Logger;

@Path("/xml")
@ApplicationScoped
public class XmlResource {

    private static final Logger LOG = Logger.getLogger(XmlResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/html-parse")
    @POST
    @Consumes(MediaType.TEXT_HTML)
    @Produces(MediaType.TEXT_PLAIN)
    public String htmlParse(String html) {
        LOG.debugf("Parsing HTML %s", html);
        return producerTemplate.requestBody(
                XmlRouteBuilder.DIRECT_HTML_TO_DOM,
                html,
                String.class);
    }

    @Path("/xslt")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String classpath(String body) throws Exception {
        return producerTemplate.requestBody("xslt:xslt/classpath-transform.xsl", body, String.class);
    }

    @Path("/html-transform")
    @POST
    @Consumes(MediaType.TEXT_HTML)
    @Produces(MediaType.TEXT_PLAIN)
    public String htmlTransform(String html) {
        LOG.debugf("Parsing HTML %s", html);
        return producerTemplate.requestBody(
                XmlRouteBuilder.DIRECT_HTML_TRANSFORM,
                html,
                String.class);
    }

    @Path("/html-to-text")
    @POST
    @Consumes(MediaType.TEXT_HTML)
    @Produces(MediaType.TEXT_PLAIN)
    public String htmlToText(String html) {
        LOG.debugf("Parsing HTML %s", html);
        return producerTemplate.requestBody(
                XmlRouteBuilder.DIRECT_HTML_TO_TEXT,
                html,
                String.class);
    }

    @Path("/xpath")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.TEXT_PLAIN)
    public String xpath(String message) {
        return producerTemplate.requestBody(XmlRouteBuilder.DIRECT_XML_CBR, message, String.class);
    }

    @Path("/xtokenize")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.TEXT_PLAIN)
    public String tokenize(String message) {
        producerTemplate.sendBody(XmlRouteBuilder.DIRECT_XTOKENIZE, message);

        StringJoiner joiner = new StringJoiner(",");
        String tokenizedXML;
        while ((tokenizedXML = consumerTemplate.receiveBody("seda:xtokenize-result", 500, String.class)) != null) {
            joiner.add(tokenizedXML);
        }
        return joiner.toString();
    }

}
