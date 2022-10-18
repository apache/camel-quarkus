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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.message.CxfConstants;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.eap.quickstarts.wscalculator.calculator.CalculatorService;

@ApplicationScoped
public class CxfSoapRoutes extends RouteBuilder {

    @Inject
    @Named("loggingFeatureClient")
    LoggingFeature loggingFeature;

    @ConfigProperty(name = "camel-quarkus.it.calculator.baseUri")
    String serviceBaseUri;

    @Override
    public void configure() {

        from("direct:simple")
                .to("cxf:bean:soapClientEndpoint?dataFormat=PAYLOAD");

        from("direct:operandsAdd")
                .setHeader(CxfConstants.OPERATION_NAME).constant("addOperands")
                .to("cxf:bean:soapClientEndpoint?dataFormat=POJO");
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
    @ApplicationScoped
    @Named
    CxfEndpoint soapClientEndpoint() {
        final CxfEndpoint result = new CxfEndpoint();
        result.setServiceClass(CalculatorService.class);
        result.setAddress(serviceBaseUri + "/calculator-ws/CalculatorService");
        result.setWsdlURL("wsdl/CalculatorService.wsdl");
        result.getFeatures().add(loggingFeature);
        return result;
    }

}
