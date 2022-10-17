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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import com.helloworld.service.CodeFirstService;
import com.helloworld.service.HelloPortType;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.cxf.ext.logging.LoggingFeature;

@ApplicationScoped
public class CxfSoapRoutes extends RouteBuilder {

    @Inject
    @Named("loggingFeatureServer")
    LoggingFeature loggingFeature;

    @Override
    public void configure() {
        /* Service */
        from("cxf:bean:soapServiceEndpoint")
                .setBody().simple("Hello ${body} from CXF service");

        from("cxf:bean:codeFirstServiceEndpoint")
                .setBody().constant("Hello CamelQuarkusCXF");
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
