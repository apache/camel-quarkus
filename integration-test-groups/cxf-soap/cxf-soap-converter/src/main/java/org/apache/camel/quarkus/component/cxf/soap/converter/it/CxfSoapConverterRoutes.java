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
package org.apache.camel.quarkus.component.cxf.soap.converter.it;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import io.quarkus.runtime.LaunchMode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.CxfPayload;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.wsdl_first.types.GetPerson;
import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.staxutils.StaxUtils;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

@ApplicationScoped
public class CxfSoapConverterRoutes extends RouteBuilder {

    public static final String GET_PERSON_MSG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<GetPerson xmlns=\"http://camel.apache.org/wsdl-first/types\" " +
            "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
            "<personId>%s</personId>" +
            "</GetPerson>";

    public static final String GET_PERSON_RESP_MSG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<GetPersonResponse xmlns=\"http://camel.apache.org/wsdl-first/types\" " +
            "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
            "<personId>%s</personId>" +
            "</GetPersonResponse>";

    @Inject
    @Named("loggingFeatureConverter")
    LoggingFeature loggingFeature;

    @Override
    public void configure() {

        from("direct:converterInvoker")
                .process(exchange -> {
                    Map<String, Object> headers = exchange.getIn().getHeaders();
                    headers.put("address", getServerUrl() + "/soapservice/PayLoadConvert/RouterPort");
                    List<Source> elements = new ArrayList<>();
                    String reqMessage = String.format(GET_PERSON_MSG, exchange.getIn().getBody(String.class));
                    elements.add(new DOMSource(StaxUtils
                            .read(new StringReader(reqMessage))
                            .getDocumentElement()));
                    CxfPayload payload = new CxfPayload<>(
                            new ArrayList<SoapHeader>(), elements, null);
                    exchange.getIn().setBody(payload);
                })
                .toD("cxf:bean:soapConverterEndpoint?address=${header.address}&dataFormat=PAYLOAD");

        from("cxf:bean:soapConverterEndpoint?dataFormat=PAYLOAD")
                .process(exchange -> {
                    // just try to turn the payload to the parameter we want
                    // to use
                    GetPerson request = exchange.getIn().getBody(GetPerson.class);

                    exchange.getMessage().setBody(String.format(GET_PERSON_RESP_MSG, request.getPersonId() + "2"));
                });

    }

    @Produces
    @SessionScoped
    @Named
    CxfEndpoint soapConverterEndpoint() {
        final CxfEndpoint result = new CxfEndpoint();
        result.getFeatures().add(loggingFeature);
        result.setServiceClass(org.apache.camel.wsdl_first.Person.class);
        //        result.setWsdlURL("wsdl/person.wsdl");
        result.setAddress("/PayLoadConvert/RouterPort");
        return result;
    }

    @Produces
    @ApplicationScoped
    @Named("loggingFeatureConverter")
    public LoggingFeature loggingFeature() {
        final LoggingFeature result = new LoggingFeature();
        result.setPrettyLogging(true);
        return result;
    }

    private static String getServerUrl() {
        Config config = ConfigProvider.getConfig();
        final int port = LaunchMode.current().equals(LaunchMode.TEST) ? config.getValue("quarkus.http.test-port", Integer.class)
                : config.getValue("quarkus.http.port", Integer.class);
        return String.format("http://localhost:%d", port);
    }

}
