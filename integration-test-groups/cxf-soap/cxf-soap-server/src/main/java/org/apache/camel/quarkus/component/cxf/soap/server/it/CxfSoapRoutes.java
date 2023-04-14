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
package org.apache.camel.quarkus.component.cxf.soap.server.it;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Element;

import com.helloworld.service.HelloPortType;
import com.sun.xml.messaging.saaj.soap.ver1_1.Message1_1Impl;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.DataFormat;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.converter.jaxp.XmlConverter;
import org.apache.camel.util.xml.StringSource;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.helpers.XPathUtils;

@ApplicationScoped
public class CxfSoapRoutes extends RouteBuilder {

    @Inject
    @Named("loggingFeatureServer")
    LoggingFeature loggingFeature;

    public static final String response = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ser=\"http://it.server.soap.cxf.component.quarkus.camel.apache.org/\">\n"
            +
            "   <soapenv:Header/>\n" +
            "   <soapenv:Body>\n" +
            "      <ser:echoResponse><return>%s</return></ser:echoResponse>\n" +
            "   </soapenv:Body>\n" +
            "</soapenv:Envelope>";

    @Override
    public void configure() {
        /* Service */
        from("cxf:bean:soapServiceEndpoint")
                .setBody().simple("Hello ${body} from CXF service");

        from(String.format("cxf:///hello-uri-address?wsdlURL=wsdl/HelloService.wsdl&serviceClass=%s",
                HelloPortType.class.getName()))
                        .setBody().simple("Hello ${body} from CXF service");

        from("cxf:bean:codeFirstServiceEndpoint")
                .choice()
                .when(simple("${header.operationName} == 'Hello'"))
                .setBody().simple("Hello ${body} code first")
                .endChoice()
                .when(simple("${header.operationName} == 'GoodBye'"))
                .setBody().simple("Good bye ${body} code first")
                .endChoice()
                .otherwise()
                .process(e -> {
                    throw new IllegalStateException("Unexpected operation " + e.getMessage().getHeader("operationName"));
                });

        from("cxf:bean:echoServiceResponseFromRouteCxfMessageDataFormat")
                .process(exchange -> {
                    SOAPMessage requestMsg = exchange.getIn().getBody(SOAPMessage.class);
                    String requestText = requestMsg.getSOAPBody().getElementsByTagName("arg0").item(0).getFirstChild()
                            .getNodeValue();
                    String xmlResponse = String.format(response, requestText + " from Camel route");
                    try (InputStream is = new ByteArrayInputStream(xmlResponse.getBytes(StandardCharsets.UTF_8))) {
                        SOAPMessage responseMsg = MessageFactory.newInstance().createMessage(null, is);

                        exchange.getIn().setBody(new Message1_1Impl(responseMsg));
                    }
                });

        from("cxf:bean:echoServiceResponseFromRouteRawDataFormat")
                .process(exchange -> {
                    String rawXmlRequest = exchange.getIn().getBody(String.class);
                    XPathUtils xu = new XPathUtils();
                    Element body = new XmlConverter().toDOMElement(new StringSource(rawXmlRequest));
                    String requestMsg = ((Element) xu.getValue("//arg0", body, XPathConstants.NODE)).getTextContent();

                    exchange.getIn().setBody(String.format(response, requestMsg + " from Camel route"));
                });

        from(String.format("cxf:echoServiceResponseFromRoute?serviceClass=%s&address=/echo-route",
                EchoServiceImpl.class.getName()))
                        .setBody(exchange -> exchange.getMessage().getBody(String.class) + " from Camel route");

        from(String.format("cxf:echoServiceResponseFromImpl?serviceClass=%s&address=/echo-impl",
                EchoServiceImpl.class.getName()))// no body set here; the response comes from EchoServiceImpl
                        .log("${body}");

    }

    @Produces
    @ApplicationScoped
    @Named("loggingFeatureServer")
    public LoggingFeature loggingFeature() {
        final LoggingFeature result = new LoggingFeature();
        result.setPrettyLogging(true);
        return result;
    }

    @Produces
    @ApplicationScoped
    @Named
    CxfEndpoint soapServiceEndpoint() {
        final CxfEndpoint result = new CxfEndpoint();
        result.setServiceClass(HelloPortType.class);
        result.setAddress("/hello-uri-bean");
        result.setWsdlURL("wsdl/HelloService.wsdl");
        result.getFeatures().add(loggingFeature);
        return result;
    }

    @Produces
    @ApplicationScoped
    @Named
    CxfEndpoint codeFirstServiceEndpoint() {
        final CxfEndpoint result = new CxfEndpoint();
        result.setServiceClass(CodeFirstService.class);
        result.setAddress("/codefirst");
        result.getFeatures().add(loggingFeature);
        return result;
    }

    @Produces
    @ApplicationScoped
    @Named
    CxfEndpoint echoServiceResponseFromRouteRawDataFormat() {
        final CxfEndpoint result = new CxfEndpoint();
        result.setServiceClass(EchoServiceImpl.class);
        result.setAddress("/echo-route-raw-data-format");
        result.setDataFormat(DataFormat.RAW);
        result.getFeatures().add(loggingFeature);
        return result;
    }

    @Produces
    @ApplicationScoped
    @Named
    CxfEndpoint echoServiceResponseFromRouteCxfMessageDataFormat() {
        final CxfEndpoint result = new CxfEndpoint();
        result.setServiceClass(EchoServiceImpl.class);
        result.setAddress("/echo-route-cxf-message-data-format");
        result.setDataFormat(DataFormat.CXF_MESSAGE);
        result.getFeatures().add(loggingFeature);
        return result;
    }

}
