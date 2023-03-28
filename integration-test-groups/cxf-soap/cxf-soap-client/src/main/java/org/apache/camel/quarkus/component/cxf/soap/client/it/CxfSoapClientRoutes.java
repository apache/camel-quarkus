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
package org.apache.camel.quarkus.component.cxf.soap.client.it;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;

import com.sun.xml.messaging.saaj.soap.ver1_1.Message1_1Impl;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.DataFormat;
import org.apache.camel.component.cxf.common.message.CxfConstants;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.eap.quickstarts.wscalculator.basicauthcalculator.BasicAuthCalculatorService;
import org.jboss.eap.quickstarts.wscalculator.calculator.CalculatorService;

@ApplicationScoped
public class CxfSoapClientRoutes extends RouteBuilder {

    @Inject
    @Named("loggingFeatureClient")
    LoggingFeature loggingFeature;

    @ConfigProperty(name = "camel-quarkus.it.calculator.baseUri")
    String serviceBaseUri;

    public static final String MESSAGE_RAW_SIMPLE_ADD = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ser=\"http://www.jboss.org/eap/quickstarts/wscalculator/Calculator\">\n"
            +
            "   <soapenv:Header/>\n" +
            "   <soapenv:Body>\n" +
            "      <ser:add>\n" +
            "         <arg0>%s</arg0>\n" +
            "         <arg1>%s</arg1>\n" +
            "      </ser:add>\n" +
            "   </soapenv:Body>\n" +
            "</soapenv:Envelope>";

    @Override
    public void configure() {

        from("direct:simpleUriBean")
                .to("cxf:bean:soapClientEndpoint?dataFormat=POJO");

        from("direct:simpleUriAddress")
                .to(String.format("cxf://%s?wsdlURL=%s&dataFormat=POJO&serviceClass=%s", calculatorServiceAddress(),
                        calculatorServiceWsdlUrl(), CalculatorService.class.getName()));

        from("direct:simpleAddDataFormat")
                .process(exchange -> {
                    Map<String, Object> headers = exchange.getIn().getHeaders();
                    String endpointDataFormat = headers.get("endpointDataFormat").toString();
                    int[] numbers = exchange.getIn().getBody(int[].class);
                    String xmlRequest = String.format(MESSAGE_RAW_SIMPLE_ADD, numbers[0], numbers[1]);
                    if (DataFormat.RAW.name().equals(endpointDataFormat)) {
                        exchange.getIn().setBody(xmlRequest);
                    } else if (DataFormat.CXF_MESSAGE.name().equals(endpointDataFormat)) {
                        try (InputStream is = new ByteArrayInputStream(xmlRequest.getBytes(StandardCharsets.UTF_8))) {
                            SOAPMessage requestMsg = MessageFactory.newInstance().createMessage(null, is);
                            exchange.getIn().setHeader(CxfConstants.OPERATION_NAME, "add");
                            exchange.getIn().setBody(new Message1_1Impl(requestMsg));
                        }
                    }
                })
                .toD("cxf:bean:soapClientEndpoint?dataFormat=${header.endpointDataFormat}");

        from("direct:operandsAdd")
                .setHeader(CxfConstants.OPERATION_NAME).constant("addOperands")
                .to("cxf:bean:soapClientEndpoint?dataFormat=POJO");

        from("direct:basicAuthAdd")
                .to("cxf:bean:basicAuthClientEndpoint?dataFormat=POJO&username={{cq.cxf.it.calculator.auth.basic.user}}&password={{cq.cxf.it.calculator.auth.basic.password}}");

        from("direct:basicAuthAddAnonymous")
                .to("cxf:bean:basicAuthClientEndpoint?dataFormat=POJO");

    }

    @Produces
    @ApplicationScoped
    @Named("loggingFeatureClient")
    public LoggingFeature loggingFeature() {
        final LoggingFeature result = new LoggingFeature();
        result.setPrettyLogging(true);
        return result;
    }

    @Produces
    @SessionScoped
    @Named
    CxfEndpoint soapClientEndpoint() {
        final CxfEndpoint result = new CxfEndpoint();
        result.setServiceClass(CalculatorService.class);
        result.setAddress(calculatorServiceAddress());
        result.setWsdlURL(calculatorServiceWsdlUrl());
        result.getFeatures().add(loggingFeature);
        return result;
    }

    @Produces
    @SessionScoped
    @Named
    CxfEndpoint basicAuthClientEndpoint() {
        final CxfEndpoint result = new CxfEndpoint();
        result.setServiceClass(BasicAuthCalculatorService.class);
        result.setAddress(serviceBaseUri + "/calculator-ws/BasicAuthCalculatorService");
        result.setWsdlURL("wsdl/BasicAuthCalculatorService.wsdl");
        result.getFeatures().add(loggingFeature);
        return result;
    }

    private String calculatorServiceAddress() {
        return serviceBaseUri + "/calculator-ws/CalculatorService";
    }

    private String calculatorServiceWsdlUrl() {
        return "wsdl/CalculatorService.wsdl";
    }

}
