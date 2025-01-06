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
package org.apache.camel.quarkus.component.cxf.soap.it.metrics;

import java.util.Optional;

import io.quarkiverse.cxf.metrics.QuarkusCxfMetricsFeature;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.DataFormat;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.quarkus.component.cxf.soap.it.metrics.service.HelloService;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class CxfSoapMetricsRoutes extends RouteBuilder {

    @ConfigProperty(name = "quarkus.http.test-port")
    String port;

    @Override
    public void configure() {

        from("direct:clientMetrics")
                .to("cxf:bean:clientMetricsEndpoint");

        from("cxf:bean:metricsServiceEndpoint")
                .process(e -> {
                    try {
                        /* We have to slow down a bit so that the native test is able to see some elapsedTime */
                        Thread.sleep(20);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    Message message = e.getMessage();
                    message.setBody("Hello " + message.getBody(String.class) + "!", String.class);
                });

    }

    @Produces
    @ApplicationScoped
    @Named
    CxfEndpoint clientMetricsEndpoint() {
        final CxfEndpoint result = new CxfEndpoint();
        result.setDataFormat(DataFormat.POJO);
        result.setServiceClass(HelloService.class);
        result.setAddress("http://localhost:" + port + "/soapservice/hello-metrics");
        result.setWsdlURL("wsdl/MetricsHelloService.wsdl");
        result.getFeatures().add(new QuarkusCxfMetricsFeature(Optional.empty()));
        return result;
    }

    @Produces
    @ApplicationScoped
    @Named
    CxfEndpoint metricsServiceEndpoint() {
        final CxfEndpoint result = new CxfEndpoint();
        result.setServiceClass(HelloService.class);
        result.setAddress("/hello-metrics");
        result.setWsdlURL("wsdl/MetricsHelloService.wsdl");
        result.getFeatures().add(new QuarkusCxfMetricsFeature(Optional.empty()));
        return result;
    }

}
