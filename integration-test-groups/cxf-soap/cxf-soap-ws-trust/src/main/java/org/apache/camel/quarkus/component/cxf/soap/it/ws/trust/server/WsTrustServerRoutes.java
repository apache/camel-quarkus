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
package org.apache.camel.quarkus.component.cxf.soap.it.ws.trust.server;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.headers.Header;

@ApplicationScoped
public class WsTrustServerRoutes extends RouteBuilder {

    @Override
    public void configure() {

        from("cxf:bean:wsTrustHelloService?dataFormat=POJO").process(new Processor() {
            public void process(final Exchange exchange) throws Exception {
                exchange.getIn().removeHeader(Header.HEADER_LIST);
                exchange.getMessage().setBody("WS-Trust Hello World!");
            }
        });
    }

    @Produces
    @ApplicationScoped
    @Named
    CxfEndpoint wsTrustHelloService() {
        final CxfEndpoint result = new CxfEndpoint();
        result.setServiceClass(TrustHelloService.class);
        result.setAddress("/jaxws-samples-wsse-policy-trust");
        result.setPortName("TrustHelloServicePort");
        result.setServiceNameAsQName(
                new QName("https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/ws-trust", "TrustHelloService"));
        result.setWsdlURL("TrustHelloService.wsdl");

        final LoggingFeature lf = new LoggingFeature();
        lf.setPrettyLogging(true);
        result.getFeatures().add(lf);

        Map<String, Object> props = new HashMap<>();

        props.put("ws-security.signature.username", "myservicekey");
        props.put("ws-security.signature.properties", "serviceKeystore.properties");
        props.put("ws-security.encryption.properties", "serviceKeystore.properties");
        props.put("ws-security.callback-handler",
                "org.apache.camel.quarkus.component.cxf.soap.it.ws.trust.server.ServerCallbackHandler");
        result.setProperties(props);

        return result;
    }

}
