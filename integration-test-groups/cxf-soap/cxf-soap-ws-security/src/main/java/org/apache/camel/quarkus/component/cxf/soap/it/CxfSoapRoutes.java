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
package org.apache.camel.quarkus.component.cxf.soap.it;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.common.ConfigurationConstants;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.jbossws.ws_extensions.wssecuritypolicy.ServiceIface;

@ApplicationScoped
public class CxfSoapRoutes extends RouteBuilder {

    @Inject
    @Named("passwordCallback")
    PasswordCallback passwordCallback;

    @Inject
    @Named("loggingFeature")
    LoggingFeature loggingFeature;

    @Inject
    @Named("wssInterceptor")
    WSS4JOutInterceptor wssInterceptor;

    @ConfigProperty(name = "camel-quarkus.it.helloWorld.baseUri")
    String serviceBaseUri;

    @Override
    public void configure() {

        from("direct:wsSecurityClient")
                .to("cxf:bean:secureEndpoint");

    }

    @Produces
    @ApplicationScoped
    @Named
    WSS4JOutInterceptor wssInterceptor() {
        final Map<String, Object> props = new HashMap<>();
        props.put(ConfigurationConstants.ACTION, "UsernameToken");
        props.put(ConfigurationConstants.PASSWORD_TYPE, "PasswordText");
        props.put(ConfigurationConstants.USER, "user1");
        props.put("passwordCallbackRef", passwordCallback);
        props.put(ConfigurationConstants.ADD_USERNAMETOKEN_NONCE, "true");
        props.put(ConfigurationConstants.ADD_USERNAMETOKEN_CREATED, "true");
        return new WSS4JOutInterceptor(props);
    }

    @Produces
    @ApplicationScoped
    @Named
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
        result.setServiceClass(ServiceIface.class);
        result.setAddress(serviceBaseUri + "/hello-ws-secured/UsernameToken/ElytronUsernameTokenImpl");
        result.setWsdlURL("wsdl/UsernameToken.wsdl");
        result.getFeatures().add(loggingFeature);
        result.getOutInterceptors().add(wssInterceptor);

        return result;
    }
}
