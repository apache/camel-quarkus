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
package org.apache.camel.quarkus.component.cxf.soap.rest.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.eap.quickstarts.wscalculator.calculator.AddOperands;
import org.jboss.eap.quickstarts.wscalculator.calculator.CalculatorService;
import org.jboss.eap.quickstarts.wscalculator.calculator.Result;

@ApplicationScoped
public class CxfSoapRestRoutes extends RouteBuilder {

    @Inject
    @Named("loggingFeatureRest")
    LoggingFeature loggingFeatureRest;

    @ConfigProperty(name = "camel-quarkus.it.calculator.baseUri")
    String serviceBaseUri;

    @Override
    public void configure() {

        rest("cxf-soap-rest").bindingMode(RestBindingMode.json)
                .post("post")
                .type(AddOperands.class)
                .outType(Result.class)
                .to("direct:headersPropagation");
        from("direct:headersPropagation")
                .process(exchange -> exchange.getIn().setBody(exchange.getIn().getBody(AddOperands.class).getArg0()))
                .to("cxf:bean:soapClientRestEndpoint?defaultOperationName=addOperands")
                .setHeader("Content-Type", constant("application/json"))
                .setBody(e -> e.getMessage().getBody(Object[].class)[0]);

    }

    @Produces
    @ApplicationScoped
    @Named("loggingFeatureRest")
    public LoggingFeature loggingFeatureRest() {
        final LoggingFeature result = new LoggingFeature();
        result.setPrettyLogging(true);
        return result;
    }

    @Produces
    @SessionScoped
    @Named
    CxfEndpoint soapClientRestEndpoint() {
        final CxfEndpoint result = new CxfEndpoint();
        result.setServiceClass(CalculatorService.class);
        result.setAddress(calculatorServiceAddress());
        result.setWsdlURL(calculatorServiceWsdlUrl());
        result.getFeatures().add(loggingFeatureRest);
        return result;
    }

    private String calculatorServiceAddress() {
        return serviceBaseUri + "/calculator-ws/CalculatorService";
    }

    private String calculatorServiceWsdlUrl() {
        return "wsdl/CalculatorService.wsdl";
    }

}
