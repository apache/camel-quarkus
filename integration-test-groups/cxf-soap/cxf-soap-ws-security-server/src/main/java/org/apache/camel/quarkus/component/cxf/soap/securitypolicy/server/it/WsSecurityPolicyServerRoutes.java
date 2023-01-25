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
package org.apache.camel.quarkus.component.cxf.soap.securitypolicy.server.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.cxf.ext.logging.LoggingFeature;

@ApplicationScoped
public class WsSecurityPolicyServerRoutes extends RouteBuilder {

    @Override
    public void configure() {

        from("cxf:bean:wsSecurityPolicyHelloService?dataFormat=POJO")
                .log("exchange: ${exchange}")
                .setBody(exchange -> "Secure good morning " + exchange.getMessage().getBody(String.class));

    }

    @Produces
    @ApplicationScoped
    @Named
    CxfEndpoint wsSecurityPolicyHelloService() {
        final CxfEndpoint result = new CxfEndpoint();
        result.setServiceClass(WssSecurityPolicyHelloServiceImpl.class);
        result.setAddress("/security-policy-hello");

        final LoggingFeature lf = new LoggingFeature();
        lf.setPrettyLogging(true);
        result.getFeatures().add(lf);

        return result;
    }

}
