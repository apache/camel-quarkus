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

import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.helloworld.service.HelloPortType;
import com.sun.xml.messaging.saaj.soap.ver1_1.Message1_1Impl;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPMessage;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.bean.BeanConstants;
import org.apache.camel.component.cxf.common.DataFormat;
import org.apache.camel.component.cxf.common.message.CxfConstants;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.converter.jaxp.XmlConverter;
import org.apache.camel.util.xml.StringSource;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.message.MessageContentsList;

@ApplicationScoped
public class CxfSoapRoutes extends RouteBuilder {

    @Inject
    @Named("loggingFeatureServer")
    LoggingFeature loggingFeature;

    public static final String response = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ser=\"http://it.server.soap.cxf.component.quarkus.camel.apache.org/\">\n"
            +
            "   <soapenv:Header/>\n" +
            "   <soapenv:Body>\n" +
            "      <ser:#methodName#Response><return>#value#</return></ser:#methodName#Response>\n" +
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

        from("cxf:bean:textServiceResponseFromRouteCxfMessageDataFormat")
                .process(exchange -> {
                    SOAPMessage requestMsg = exchange.getIn().getBody(SOAPMessage.class);
                    Node argNode = requestMsg.getSOAPBody().getElementsByTagName("arg0").item(0);
                    String inputArg = argNode.getFirstChild()
                            .getNodeValue();
                    String operation = argNode.getParentNode().getLocalName();
                    String xmlResponse = response.replaceAll("#methodName#", operation).replaceAll("#value#",
                            alterTextByTextOperation(operation, inputArg));
                    try (InputStream is = new ByteArrayInputStream(xmlResponse.getBytes(StandardCharsets.UTF_8))) {
                        SOAPMessage responseMsg = MessageFactory.newInstance().createMessage(null, is);

                        exchange.getIn().setBody(new Message1_1Impl(responseMsg));
                    }
                });

        from("cxf:bean:textServiceResponseFromRouteRawDataFormat")
                .process(exchange -> {
                    String rawXmlRequest = exchange.getIn().getBody(String.class);
                    XPathUtils xu = new XPathUtils();
                    Element body = new XmlConverter().toDOMElement(new StringSource(rawXmlRequest));
                    Node argNode = ((Element) xu.getValue("//arg0", body, XPathConstants.NODE));
                    String operation = argNode.getParentNode().getLocalName();
                    String inputArg = argNode.getTextContent();

                    exchange.getIn().setBody(response.replaceAll("#methodName#", operation).replaceAll("#value#",
                            alterTextByTextOperation(operation, inputArg)));
                });

        from(String.format("cxf:textServiceResponseFromRoute?serviceClass=%s&address=/text-service-route",
                TextService.class.getName()))
                .process(exchange -> {
                    String operation = (String) exchange.getIn().getHeader(CxfConstants.OPERATION_NAME);
                    String inputArg = ((MessageContentsList) exchange.getIn().getBody()).get(0).toString();
                    exchange.getIn().setBody(alterTextByTextOperation(operation, inputArg));
                });

        from(String.format("cxf:textServiceResponseFromImpl?serviceClass=%s&address=/text-service-impl",
                TextService.class.getName()))
                .process(exchange -> exchange.getIn().setHeader(BeanConstants.BEAN_METHOD_NAME,
                        exchange.getIn().getHeader(CxfConstants.OPERATION_NAME)))
                .to(String.format("bean:%s", TextServiceImpl.class.getName()));
    }

    private String alterTextByTextOperation(String operation, String text) {
        return switch (operation) {
        case "lowerCase" -> text.toLowerCase();
        case "upperCase" -> text.toUpperCase();
        default -> null;
        };
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
    CxfEndpoint textServiceResponseFromRouteRawDataFormat() {
        final CxfEndpoint result = new CxfEndpoint();
        result.setServiceClass(TextService.class);
        result.setAddress("/text-route-raw-data-format");
        result.setDataFormat(DataFormat.RAW);
        result.getFeatures().add(loggingFeature);
        return result;
    }

    @Produces
    @ApplicationScoped
    @Named
    CxfEndpoint textServiceResponseFromRouteCxfMessageDataFormat() {
        final CxfEndpoint result = new CxfEndpoint();
        result.setServiceClass(TextService.class);
        result.setAddress("/text-route-cxf-message-data-format");
        result.setDataFormat(DataFormat.CXF_MESSAGE);
        result.getFeatures().add(loggingFeature);
        return result;
    }

}
