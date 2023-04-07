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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.jboss.logging.Logger;

@Path("/xml")
@ApplicationScoped
public class XsltResource {

    private static final Logger LOG = Logger.getLogger(XsltResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Inject
    CamelContext camelContext;

    @Path("/xslt")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String classpath(@QueryParam("output") @DefaultValue("string") String output, String body) {
        if (output.equals("file")) {
            return producerTemplate.requestBodyAndHeader("xslt:xslt/classpath-transform.xsl?output=file",
                    body, Exchange.XSLT_FILE_NAME, "target/xsltme.xml", String.class);
        }
        return producerTemplate.requestBody("xslt:xslt/classpath-transform.xsl?output=" + output, body, String.class);
    }

    @Path("/xslt_include")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String xsltInclude(String body) {
        return producerTemplate.requestBody("xslt:xslt/include.xsl", body, String.class);
    }

    @Path("/xslt_terminate")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String xsltTerminate(String body) {
        Exchange out = producerTemplate.request("xslt:xslt/terminate.xsl", exchange -> exchange.getIn().setBody(body));
        Exception warning = out.getProperty(Exchange.XSLT_WARNING, Exception.class);
        return warning.getMessage();
    }

    @Path("/xslt-extension-function")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String extensionFunction(String body) {
        return producerTemplate.requestBody("xslt:xslt/extension-function.xsl", body, String.class);
    }

    @Path("/xslt-custom-uri-resolver")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String customURIResolver(String body) {
        return producerTemplate.requestBody("xslt:xslt/include_not_existing_resource.xsl?uriResolver=#customURIResolver", body,
                String.class);
    }

    @Path("/aggregate")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String aggregate() throws Exception {
        MockEndpoint mock = camelContext.getEndpoint("mock:transformed", MockEndpoint.class);
        mock.expectedMessageCount(1);

        producerTemplate.sendBody("direct:aggregate", "<item>A</item>");
        producerTemplate.sendBody("direct:aggregate", "<item>B</item>");
        producerTemplate.sendBody("direct:aggregate", "<item>C</item>");

        mock.assertIsSatisfied();
        return mock.getExchanges().get(0).getIn().getBody(String.class);

    }

    @Path("/html-transform")
    @POST
    @Consumes(MediaType.TEXT_HTML)
    @Produces(MediaType.TEXT_PLAIN)
    public String htmlTransform(String html) {
        LOG.debugf("Parsing HTML %s", html);
        return producerTemplate.requestBody(
                XsltRouteBuilder.DIRECT_HTML_TRANSFORM,
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
                XsltRouteBuilder.DIRECT_HTML_TO_TEXT,
                html,
                String.class);
    }

    @Path("/xpath")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.TEXT_PLAIN)
    public String xpath(String message) {
        return producerTemplate.requestBody(XsltRouteBuilder.DIRECT_XML_CBR, message, String.class);
    }

    @Path("/xtokenize")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.TEXT_PLAIN)
    public String tokenize(String message) {
        producerTemplate.sendBody(XsltRouteBuilder.DIRECT_XTOKENIZE, message);

        StringJoiner joiner = new StringJoiner(",");
        String tokenizedXML;
        while ((tokenizedXML = consumerTemplate.receiveBody("seda:xtokenize-result", 500, String.class)) != null) {
            joiner.add(tokenizedXML);
        }
        return joiner.toString();
    }
}
