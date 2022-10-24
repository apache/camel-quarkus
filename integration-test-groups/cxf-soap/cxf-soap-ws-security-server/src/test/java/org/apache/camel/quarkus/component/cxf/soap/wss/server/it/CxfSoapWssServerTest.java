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
package org.apache.camel.quarkus.component.cxf.soap.wss.server.it;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil.anyNs;

@QuarkusTest
@QuarkusTestResource(CxfWssServerTestResource.class)
class CxfSoapWssServerTest {

    @Test
    void wsdl() throws IOException {
        /* We do not set any static WSDL resource via org.apache.camel.component.cxf.jaxws.CxfEndpoint.setWsdlURL(String)
         * in org.apache.camel.quarkus.component.cxf.soap.wss.server.it.CxfSoapWssServerRoutes.wssRounderService()
         * So let's check whether the auto-generated WSDL is served properly */
        RestAssured.given()
                .get("/soapservice/rounder?wsdl")
                .then()
                .statusCode(200)
                .body(
                        Matchers.hasXPath(
                                anyNs("definitions", "portType")
                                        + "[@name = 'WssRounderService']"
                                        + anyNs("operation") + "/@name",
                                org.hamcrest.CoreMatchers.is("round")));
    }

    @Test
    void usernameTokenCorrectPassword() throws IOException {

        final Config config = ConfigProvider.getConfig();
        final String username = config.getValue("camel-quarkus.wss.server.username", String.class);
        final String password = config.getValue("camel-quarkus.wss.server.password", String.class);

        final WssRounderService client = rounderClient(username, password);

        Assertions.assertThat(client.round(2.1)).isEqualTo(2);

    }

    @Test
    void usernameTokenBadPassword() throws IOException {

        final Config config = ConfigProvider.getConfig();
        final String username = config.getValue("camel-quarkus.wss.server.username", String.class);
        final WssRounderService client = rounderClient(username, "fakePassword");

        Assertions.assertThatExceptionOfType(javax.xml.ws.soap.SOAPFaultException.class)
                .isThrownBy(() -> client.round(2.8))
                .withMessage(
                        "A security error was encountered when verifying the message");

    }

    static WssRounderService rounderClient(String username, String password) {
        final WssRounderService client = QuarkusCxfClientTestUtil.getClient(WssRounderService.class, "/soapservice/rounder");

        final CallbackHandler passwordCallback = new CallbackHandler() {
            @Override
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                for (Callback callback : callbacks) {
                    if (callback instanceof WSPasswordCallback) {
                        ((WSPasswordCallback) callback).setPassword(password);
                        break;
                    }
                }
            }
        };

        final Map<String, Object> props = new HashMap<>();
        props.put(ConfigurationConstants.ACTION, "UsernameToken");
        props.put(ConfigurationConstants.PASSWORD_TYPE, "PasswordText");
        props.put(ConfigurationConstants.USER, username);
        props.put(ConfigurationConstants.PW_CALLBACK_REF, passwordCallback);

        Client clientProxy = ClientProxy.getClient(client);
        clientProxy.getOutInterceptors().add(new WSS4JOutInterceptor(props));
        return client;
    }

    @Test
    void anonymous() throws IOException {
        final WssRounderService client = QuarkusCxfClientTestUtil.getClient(WssRounderService.class, "/soapservice/rounder");
        /* Make sure that it fails properly when called without a password */
        Assertions.assertThatExceptionOfType(javax.xml.ws.soap.SOAPFaultException.class)
                .isThrownBy(() -> client.round(2.8))
                .withMessage(
                        "A security error was encountered when verifying the message");

    }

}
