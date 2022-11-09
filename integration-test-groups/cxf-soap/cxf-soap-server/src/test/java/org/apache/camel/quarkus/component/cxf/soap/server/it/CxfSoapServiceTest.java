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

import javax.xml.ws.BindingProvider;

import com.helloworld.service.HelloPortType;
import com.helloworld.service.HelloService;
import io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@QuarkusTest
class CxfSoapServiceTest {

    @ParameterizedTest
    @ValueSource(strings = { "uri-bean", "uri-address" })
    public void simpleSoapService(String uriEndpoint) {
        final HelloService service = new HelloService();
        final HelloPortType helloPort = service.getHelloPort();
        String endpointURL = getServerUrl() + "/soapservice/hello-" + uriEndpoint;
        ((BindingProvider) helloPort).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointURL);
        Assertions.assertEquals(helloPort.hello("World"), "Hello World from CXF service");
    }

    private static String getServerUrl() {
        Config config = ConfigProvider.getConfig();
        final int port = LaunchMode.current().equals(LaunchMode.TEST) ? config.getValue("quarkus.http.test-port", Integer.class)
                : config.getValue("quarkus.http.port", Integer.class);
        return String.format("http://localhost:%d", port);
    }

    @Test
    public void testCodeFirstSoapService() {

        final String response = RestAssured.given()
                .contentType(ContentType.XML)
                .body("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ser=\"http://www.helloworld.com/CodeFirstService/\">\n"
                        +
                        "   <soapenv:Header/>\n" +
                        "   <soapenv:Body>\n" +
                        "      <ser:HelloRequest>HelloWorld</ser:HelloRequest>\n" +
                        "   </soapenv:Body>\n" +
                        "</soapenv:Envelope>")
                .post("/soapservice/codefirst")
                .then()
                .statusCode(200)
                .assertThat()
                .extract().asString();

        org.junit.jupiter.api.Assertions.assertTrue(response.contains("Hello CamelQuarkusCXF"));
    }

    @Test
    public void echoServiceResponseFromRoute() {
        /* We setServiceClass(EchoServiceImpl.class) in org.apache.camel.quarkus.component.cxf.soap.server.it.CxfSoapRoutes.echoServiceResponseFromRoute()
         * and at the same time we set the body in the associated Camel route definition. What we do in the route should have a higher prio */
        final EchoService echo = QuarkusCxfClientTestUtil.getClient(EchoService.class, "/soapservice/echo-route");
        Assertions.assertEquals("Hello there! from Camel route", echo.echo("Hello there!"));
    }

    @Test
    public void echoServiceResponseFromImpl() {
        /* We setServiceClass(EchoServiceImpl.class) in org.apache.camel.quarkus.component.cxf.soap.server.it.CxfSoapRoutes.echoServiceResponseFromImpl()
         * but we do not set the body in the associated Camel route definition. Hence the response should come from EchoServiceImpl */
        final EchoService echo = QuarkusCxfClientTestUtil.getClient(EchoService.class, "/soapservice/echo-impl");
        Assertions.assertEquals("Hello there!", echo.echo("Hello there!"));
    }

}
