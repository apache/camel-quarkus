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
package org.apache.camel.quarkus.component.cxf.soap.it.ws.trust;

import java.net.URL;
import java.util.Map;

import javax.xml.namespace.QName;

import io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import org.apache.camel.quarkus.component.cxf.soap.it.ws.trust.server.TrustHelloService;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.cxf.ws.security.trust.TrustException;
import org.assertj.core.api.Assertions;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil.anyNs;
import static io.restassured.RestAssured.given;

@QuarkusTest
public class CxfWsTrustTest {

    /**
     * Make sure the ws-trust-1.4-service.wsdl file is served
     */
    @Test
    void stsWsdl() {
        RestAssuredConfig config = RestAssured.config();
        config.getXmlConfig().namespaceAware(false);
        given()
                .config(config)
                .when().get("/soapservice/jaxws-samples-wsse-policy-trust-sts?wsdl")
                .then()
                .statusCode(200)
                .body(
                        Matchers.hasXPath(
                                anyNs("definitions", "Policy")
                                        + "/@*[local-name() = 'Id']",
                                CoreMatchers.is("UT_policy")));
    }

    @Test
    void wsdl() {
        RestAssuredConfig config = RestAssured.config();
        config.getXmlConfig().namespaceAware(false);
        given()
                .config(config)
                .when().get("/soapservice/jaxws-samples-wsse-policy-trust?wsdl")
                .then()
                .statusCode(200)
                .body(
                        Matchers.hasXPath(
                                anyNs("definitions", "Policy")
                                        + "[1]/@*[local-name() = 'Id']",
                                CoreMatchers.is("AsymmetricSAML2Policy")),
                        Matchers.hasXPath(
                                anyNs("definitions", "Policy")
                                        + "[2]/@*[local-name() = 'Id']",
                                CoreMatchers.is("Input_Policy")),
                        Matchers.hasXPath(
                                anyNs("definitions", "Policy")
                                        + "[3]/@*[local-name() = 'Id']",
                                CoreMatchers.is("Output_Policy"))

                );
    }

    @Test
    public void programmaticSts() throws Exception {
        Bus bus = BusFactory.newInstance().createBus();
        try {
            BusFactory.setThreadDefaultBus(bus);

            TrustHelloService proxy = trustHelloService();

            final QName stsServiceName = new QName("http://docs.oasis-open.org/ws-sx/ws-trust/200512/", "SecurityTokenService");
            final QName stsPortName = new QName("http://docs.oasis-open.org/ws-sx/ws-trust/200512/", "UT_Port");

            String stsURL = QuarkusCxfClientTestUtil.getServerUrl()
                    + "/soapservice/jaxws-samples-wsse-policy-trust-sts/SecurityTokenService?wsdl";
            setupWsseAndSTSClient(proxy, bus, stsURL, stsServiceName, stsPortName);

            Assertions.assertThat(proxy.sayHello()).isEqualTo("WS-Trust Hello World!");
        } finally {
            bus.shutdown(true);
        }

    }

    @Test
    public void programmaticStsNotSet() throws Exception {
        TrustHelloService proxy = trustHelloService();
        Assertions.assertThatThrownBy(proxy::sayHello).hasCauseInstanceOf(TrustException.class);
    }

    TrustHelloService trustHelloService() throws Exception {
        final QName serviceName = new QName("https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/ws-trust",
                "TrustHelloService");
        final URL wsdlURL = new URL(io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil.getServerUrl()
                + "/soapservice/jaxws-samples-wsse-policy-trust/TrustHelloService?wsdl");
        Service service = Service.create(wsdlURL, serviceName);
        return (TrustHelloService) service.getPort(TrustHelloService.class);
    }

    public static void setupWsseAndSTSClient(TrustHelloService proxy, Bus bus, String stsWsdlLocation, QName stsService,
            QName stsPort) {
        Map<String, Object> ctx = ((BindingProvider) proxy).getRequestContext();
        setServiceContextAttributes(ctx);
        ctx.put(SecurityConstants.STS_CLIENT, createSTSClient(bus, stsWsdlLocation, stsService, stsPort));
    }

    private static void setServiceContextAttributes(Map<String, Object> ctx) {
        ctx.put(SecurityConstants.CALLBACK_HANDLER, new ClientCallbackHandler());
        ctx.put(SecurityConstants.SIGNATURE_PROPERTIES,
                Thread.currentThread().getContextClassLoader().getResource("clientKeystore.properties"));
        ctx.put(SecurityConstants.ENCRYPT_PROPERTIES,
                Thread.currentThread().getContextClassLoader().getResource("clientKeystore.properties"));
        ctx.put(SecurityConstants.SIGNATURE_USERNAME, "myclientkey");
        ctx.put(SecurityConstants.ENCRYPT_USERNAME, "myservicekey");
    }

    /**
     * Create and configure an STSClient for use by service TrustHelloServiceImpl.
     *
     * Whenever an "<sp:IssuedToken>" policy is configured on a WSDL port, as is the
     * case for TrustHelloServiceImpl, a STSClient must be created and configured in
     * order for the service to connect to the STS-server to obtain a token.
     *
     * @param  bus
     * @param  stsWsdlLocation
     * @param  stsService
     * @param  stsPort
     * @return
     */
    private static STSClient createSTSClient(Bus bus, String stsWsdlLocation, QName stsService, QName stsPort) {
        STSClient stsClient = new STSClient(bus);
        if (stsWsdlLocation != null) {
            stsClient.setWsdlLocation(stsWsdlLocation);
            stsClient.setServiceQName(stsService);
            stsClient.setEndpointQName(stsPort);
        }
        Map<String, Object> props = stsClient.getProperties();
        props.put(SecurityConstants.USERNAME, "alice");
        props.put(SecurityConstants.CALLBACK_HANDLER, new ClientCallbackHandler());
        props.put(SecurityConstants.ENCRYPT_PROPERTIES,
                Thread.currentThread().getContextClassLoader().getResource("clientKeystore.properties"));
        props.put(SecurityConstants.ENCRYPT_USERNAME, "mystskey");
        props.put(SecurityConstants.STS_TOKEN_USERNAME, "myclientkey");
        props.put(SecurityConstants.STS_TOKEN_PROPERTIES,
                Thread.currentThread().getContextClassLoader().getResource("clientKeystore.properties"));
        props.put(SecurityConstants.STS_TOKEN_USE_CERT_FOR_KEYINFO, "true");
        return stsClient;
    }
}
