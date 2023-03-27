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
package org.apache.camel.quarkus.component.cxf.soap.wsrm.it;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import io.quarkus.runtime.LaunchMode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.jaxws.CxfConfigurer;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.frontend.AbstractWSDLBasedEndpointFactory;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.WSAContextUtils;
import org.apache.cxf.ws.rm.manager.AcksPolicyType;
import org.apache.cxf.ws.rm.manager.DestinationPolicyType;
import org.apache.cxf.ws.rmp.v200502.RMAssertion;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

@ApplicationScoped
public class CxfSoapWsrmRoutes extends RouteBuilder {

    @Inject
    @Named("loggingFeatureWsrm")
    LoggingFeature loggingFeature;

    @Inject
    @Named("resultsNoWsrm")
    Queue<String> resultsNoWsrm;

    @Inject
    @Named("resultsWsrm")
    Queue<String> resultsWsrm;

    @Override
    public void configure() {

        from("seda:wsrmInvoker")
                .process(exchange -> {
                    boolean enableWsrm = exchange.getIn().getHeader("enableWsrm", Boolean.class);

                    Map<String, Object> headers = exchange.getIn().getHeaders();
                    headers.put("address", getServerUrl() + "/soapservice/" + (enableWsrm ? "wsrm" : "noWsrm") + "/RouterPort");

                    headers.put("endpoint", (enableWsrm ? "wsrm" : "noWsrm") + "ClientEndpoint");
                })
                .toD("cxf:bean:${header.endpoint}?address=${header.address}");

        from("cxf:bean:wsrmServerEndpoint")
                .process("responseProcessorWsrm")
                .process(e -> resultsWsrm.add(e.getIn().getBody(String.class)));

        from("cxf:bean:noWsrmServerEndpoint")
                .process("responseProcessorWsrm")
                .process(e -> resultsNoWsrm.add(e.getIn().getBody(String.class)));

    }

    @Produces
    @ApplicationScoped
    @Named("noWsrmClientEndpoint")
    CxfEndpoint noWwsrmClientEndpoint(@Named("resultsNoWsrm") Queue<String> wsrmResults) {
        final CxfEndpoint cxfEndpoint = new CxfEndpoint();
        cxfEndpoint.getFeatures().add(loggingFeature);
        cxfEndpoint.setServiceClass(GreeterService.class);
        cxfEndpoint.setAddress("/nowsrm/RouterPort");
        cxfEndpoint.getFeatures().add(new org.apache.cxf.ws.addressing.WSAddressingFeature());
        //simulate lost messages
        cxfEndpoint.getOutInterceptors().add(new MessageLossSimulator(wsrmResults));
        return cxfEndpoint;
    }

    @Produces
    @ApplicationScoped
    @Named("wsrmClientEndpoint")
    CxfEndpoint wsrmClientEndpoint(@Named("resultsWsrm") Queue<String> wsrmResults) {
        CxfEndpoint cxfEndpoint = noWwsrmClientEndpoint(wsrmResults);
        cxfEndpoint.setAddress("/wsrm/RouterPort");

        addWsRmFeature(cxfEndpoint);
        cxfEndpoint.setCxfConfigurer(new MyCxfConfigurer());
        return cxfEndpoint;
    }

    @Produces
    @ApplicationScoped
    @Named("noWsrmServerEndpoint")
    CxfEndpoint noWsrmServerEndpoint() {

        CxfEndpoint cxfEndpoint = new CxfEndpoint();
        cxfEndpoint.setServiceClass(GreeterService.class);
        cxfEndpoint.getFeatures().add(loggingFeature);
        cxfEndpoint.setAddress("/noWsrm/RouterPort");
        cxfEndpoint.getFeatures().add(new org.apache.cxf.ws.addressing.WSAddressingFeature());

        return cxfEndpoint;
    }

    @Produces
    @ApplicationScoped
    @Named("wsrmServerEndpoint")
    CxfEndpoint wsrmServerEndpoint() {

        CxfEndpoint cxfEndpoint = noWsrmServerEndpoint();
        cxfEndpoint.setAddress("/wsrm/RouterPort");
        addWsRmFeature(cxfEndpoint);

        return cxfEndpoint;
    }

    @Produces
    @ApplicationScoped
    @Named("greeterServiceWsrm")
    GreeterService greeterService() {
        return new GreeterImpl();
    }

    @Produces
    @ApplicationScoped
    @Named("loggingFeatureWsrm")
    public LoggingFeature loggingFeature() {
        final LoggingFeature result = new LoggingFeature();
        result.setPrettyLogging(true);
        return result;
    }

    @ApplicationScoped
    @Named("responseProcessorWsrm")
    Processor responseProcessorWsrm(@Named("greeterServiceWsrm") GreeterService greeterService) {

        return exchange -> {
            String resp = greeterService.greetMe(exchange.getIn().getBody(String.class));
            exchange.getIn().setBody(resp);
        };
    }

    @Produces
    @ApplicationScoped
    @Named("resultsNoWsrm")
    Queue<String> resultsNoWsrm() {
        return new ConcurrentLinkedDeque<>();
    }

    @Produces
    @ApplicationScoped
    @Named("resultsWsrm")
    Queue<String> resultsWsrm() {
        return new ConcurrentLinkedDeque<>();
    }

    private void addWsRmFeature(CxfEndpoint result) {
        org.apache.cxf.ws.rm.feature.RMFeature rmFeature = new org.apache.cxf.ws.rm.feature.RMFeature();
        RMAssertion.BaseRetransmissionInterval baseRetransmissionInterval = new RMAssertion.BaseRetransmissionInterval();
        baseRetransmissionInterval.setMilliseconds(Long.valueOf(4000));
        RMAssertion.AcknowledgementInterval acknowledgementInterval = new RMAssertion.AcknowledgementInterval();
        acknowledgementInterval.setMilliseconds(Long.valueOf(2000));

        RMAssertion rmAssertion = new RMAssertion();
        rmAssertion.setAcknowledgementInterval(acknowledgementInterval);
        rmAssertion.setBaseRetransmissionInterval(baseRetransmissionInterval);

        AcksPolicyType acksPolicy = new AcksPolicyType();
        acksPolicy.setIntraMessageThreshold(0);
        DestinationPolicyType destinationPolicy = new DestinationPolicyType();
        destinationPolicy.setAcksPolicy(acksPolicy);

        rmFeature.setRMAssertion(rmAssertion);
        rmFeature.setDestinationPolicy(destinationPolicy);

        result.getFeatures().add(rmFeature);
    }

    private static String getServerUrl() {
        Config config = ConfigProvider.getConfig();
        final int port = LaunchMode.current().equals(LaunchMode.TEST)
                ? config.getValue("quarkus.http.test-port", Integer.class)
                : config.getValue("quarkus.http.port", Integer.class);
        return String.format("http://localhost:%d", port);
    }

    public class MyCxfConfigurer implements CxfConfigurer {

        @Override
        public void configure(AbstractWSDLBasedEndpointFactory factoryBean) {
            // Do nothing here
        }

        @Override
        public void configureClient(Client client) {

            String decoupledEndpoint = "/wsrm/decoupled_endpoint";
            HTTPConduit hc = (HTTPConduit) (client.getConduit());
            HTTPClientPolicy cp = hc.getClient();
            cp.setDecoupledEndpoint(decoupledEndpoint);
            client.getBus().setProperty(WSAContextUtils.DECOUPLED_ENDPOINT_BASE_PROPERTY,
                    getServerUrl()
                            + "/soapservice");

        }

        @Override
        public void configureServer(Server server) {
            // Do nothing here

        }

    }
}
