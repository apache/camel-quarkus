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

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.StringJoiner;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

@Path("/xml")
@ApplicationScoped
public class XmlResource {

    private static final Logger LOG = Logger.getLogger(XmlResource.class);

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

    @Path("/xslt-ref")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String xsltRef(String body) {
        return producerTemplate.requestBody("xslt:ref:xslt_resource", body, String.class);
    }

    @Path("/xslt-bean")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String xsltBean(String body) {
        return producerTemplate.requestBody("xslt:bean:xslt_bean.getXsltResource", body, String.class);
    }

    @Path("/xslt-file")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String xsltFile(String body) throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("xslt/classpath-transform.xsl")) {
            File file = File.createTempFile("xslt", ".xsl");
            Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return producerTemplate.requestBody("xslt:file:" + file, body, String.class);
        }
    }

    @Path("/xslt-http")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String xsltHttp(String body) {
        String serverURL = ConfigProvider.getConfig()
                .getConfigValue("xslt.server-url")
                .getRawValue();
        return producerTemplate.requestBody("xslt:" + serverURL + "/xslt", body, String.class);
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
