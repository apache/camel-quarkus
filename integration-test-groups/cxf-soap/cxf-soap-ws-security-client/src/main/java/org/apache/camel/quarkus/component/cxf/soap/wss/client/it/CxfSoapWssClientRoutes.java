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
package org.apache.camel.quarkus.component.cxf.soap.wss.client.it;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.common.ConfigurationConstants;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.eap.quickstarts.wscalculator.wsscalculator.WssCalculatorService;

@ApplicationScoped
public class CxfSoapWssClientRoutes extends RouteBuilder {

    @ConfigProperty(name = "camel-quarkus.it.wss.client.username")
    String username;

    @Inject
    @Named("passwordCallback")
    PasswordCallback passwordCallback;

    @Inject
    @Named("loggingFeatureSecurity")
    LoggingFeature loggingFeature;

    @Inject
    @Named("wssInterceptor")
    WSS4JOutInterceptor wssInterceptor;

    @ConfigProperty(name = "camel-quarkus.it.wss.client.baseUri")
    String serviceBaseUri;

    @Override
    public void configure() {

        from("direct:modulo")
                .to("cxf:bean:secureEndpoint?dataFormat=POJO");

    }

    @Produces
    @ApplicationScoped
    @Named
    WSS4JOutInterceptor wssInterceptor() {
        final Map<String, Object> props = new HashMap<>();
        props.put(ConfigurationConstants.ACTION, "UsernameToken");
        props.put(ConfigurationConstants.PASSWORD_TYPE, "PasswordText");
        props.put(ConfigurationConstants.USER, username);
        props.put(ConfigurationConstants.PW_CALLBACK_REF, passwordCallback);
        props.put(ConfigurationConstants.ADD_USERNAMETOKEN_NONCE, "true");
        props.put(ConfigurationConstants.ADD_USERNAMETOKEN_CREATED, "true");
        return new WSS4JOutInterceptor(props);
    }

    @Produces
    @ApplicationScoped
    @Named("loggingFeatureSecurity")
    public LoggingFeature loggingFeature() {
        final LoggingFeature result = new LoggingFeature();
        result.setPrettyLogging(true);
        return result;
    }

    @Produces
    @ApplicationScoped
    @Named
    CxfEndpoint secureEndpoint() {
        final CxfEndpoint result = new CxfEndpoint();
        result.setServiceClass(WssCalculatorService.class);
        result.setAddress(serviceBaseUri + "/calculator-ws/WssCalculatorService");
        result.setWsdlURL("wsdl/WssCalculatorService.wsdl");
        result.getFeatures().add(loggingFeature);
        result.getOutInterceptors().add(wssInterceptor);

        return result;
    }
}
