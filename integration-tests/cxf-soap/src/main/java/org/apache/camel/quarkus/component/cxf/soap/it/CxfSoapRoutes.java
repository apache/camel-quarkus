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

import com.helloworld.service.CodeFirstService;
import com.helloworld.service.HelloPortType;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.message.CxfConstants;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.common.ConfigurationConstants;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class CxfSoapRoutes extends RouteBuilder {

    @Inject
    @Named("passwordCallback")
    PasswordCallback passwordCallback;

    @Inject
    @Named("loggingFeature")
    LoggingFeature loggingFeature;

    @Inject
    @Named("wsAddressingFeature")
    WSAddressingFeature wsAddressingFeature;

    @Inject
    @Named("wssInterceptor")
    WSS4JOutInterceptor wssInterceptor;

    @ConfigProperty(name = "wiremock.url")
    String serviceBaseUri;

    @Override
    public void configure() {

        /* Client */
        from("direct:simpleSoapClient")
                .to("cxf:bean:soapClientEndpoint?dataFormat=POJO");

        from("direct:wsSecurityClient")
                .to("cxf:bean:secureEndpoint?dataFormat=POJO");

        from("direct:complexSoapClient")
                .setHeader(CxfConstants.OPERATION_NAME).constant("Person")
                .to("cxf:bean:soapClientEndpoint?dataFormat=POJO");

        /* Service */
        from("cxf:bean:soapServiceEndpoint")
                .setBody().simple("Hello ${body} from CXF service");

        from("cxf:bean:codeFirstServiceEndpoint")
                .setBody().constant("Hello CamelQuarkusCXF");
    }

    @Produces
    @ApplicationScoped
    @Named
    WSS4JOutInterceptor wssInterceptor() {
        final Map<String, Object> props = new HashMap<>();
        props.put(ConfigurationConstants.ACTION, "UsernameToken");
        props.put(ConfigurationConstants.PASSWORD_TYPE, "PasswordText");
        props.put(ConfigurationConstants.USER, "camel");
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
    public WSAddressingFeature wsAddressingFeature() {
        final WSAddressingFeature result = new WSAddressingFeature();
        return result;
    }

    @Produces
    @ApplicationScoped
    @Named
    CxfEndpoint secureEndpoint() {
        final CxfEndpoint result = new CxfEndpoint();
        result.setServiceClass(HelloPortType.class);
        result.setAddress(serviceBaseUri + "/hellowss");
        result.setWsdlURL("wsdl/HelloService.wsdl");
        result.getFeatures().add(loggingFeature);
        result.getFeatures().add(wsAddressingFeature);
        result.getOutInterceptors().add(wssInterceptor);
        return result;
    }

    @Produces
    @ApplicationScoped
    @Named
    CxfEndpoint soapClientEndpoint() {
        final CxfEndpoint result = new CxfEndpoint();
        result.setServiceClass(HelloPortType.class);
        result.setAddress(serviceBaseUri + "/hello");
        result.setWsdlURL("wsdl/HelloService.wsdl");
        result.getFeatures().add(loggingFeature);
        return result;
    }

    @Produces
    @ApplicationScoped
    @Named
    CxfEndpoint soapServiceEndpoint() {
        final CxfEndpoint result = new CxfEndpoint();
        result.setServiceClass(HelloPortType.class);
        result.setAddress("/hello");
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
}
