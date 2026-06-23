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

import com.helloworld.service.HelloPortType;
import com.helloworld.service.HelloService;
import io.quarkiverse.cxf.test.QuarkusCxfTestUtil;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import jakarta.xml.ws.BindingProvider;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.quarkiverse.cxf.test.internal.QuarkusCxfInternalTestUtil.anyNs;
import static io.restassured.RestAssured.given;

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
    public void codeFirstWsdl() {

        RestAssuredConfig config = RestAssured.config();
        config.getXmlConfig().namespaceAware(false);
        RestAssured.given()
                .config(config)
                .when().get("/soapservice/codefirst?wsdl")
                .then()
                .statusCode(200)
                .body(
                        /* Make sure that the two operations are available in the generated WSDL */
                        Matchers.hasXPath(
                                anyNs("definitions", "binding", "operation")
                                        + "[1]/@*[local-name() = 'name']",
                                CoreMatchers.is("GoodBye")),
                        Matchers.hasXPath(
                                anyNs("definitions", "binding", "operation")
                                        + "[2]/@*[local-name() = 'name']",
                                CoreMatchers.is("Hello")));
    }

    @Test
    public void codeFirstSoapService() {
        final CodeFirstService client = QuarkusCxfTestUtil.getClient(CodeFirstService.TARGET_NS, CodeFirstService.class,
                "/soapservice/codefirst");
        Assertions.assertEquals("Hello Joe code first", client.hello("Joe"));
        Assertions.assertEquals("Good bye Laszlo code first", client.goodBye("Laszlo"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "raw", "cxf-message" })
    public void testCodeFirstSoapServiceDataFormats(String dataFormat) {
        final TextService textService = QuarkusCxfTestUtil.getClient(TextService.class,
                String.format("/soapservice/text-route-%s-data-format", dataFormat));
        assertTextService(textService, "Hello there from text service and " + dataFormat);
    }

    @Test
    public void echoServiceResponseFromRoute() {
        final TextService textService = QuarkusCxfTestUtil.getClient(TextService.class,
                "/soapservice/text-service-route");
        assertTextService(textService, "Hello there from text service route!");
    }

    @Test
    public void echoServiceResponseFromImpl() {
        final TextService textService = QuarkusCxfTestUtil.getClient(TextService.class, "/soapservice/text-service-impl");
        assertTextService(textService, "Hello there from text service impl!");
    }

    private void assertTextService(TextService textService, String input) {
        Assertions.assertEquals(input.toUpperCase(), textService.upperCase(input));
        Assertions.assertEquals(input.toLowerCase(), textService.lowerCase(input));
    }

    @Test
    public void wsAddressingWithHeaders() {
        final String soapRequest = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\""
                + " xmlns:wsa=\"http://www.w3.org/2005/08/addressing\""
                + " xmlns:ns=\"" + WsAddressingService.TARGET_NS + "\">\n"
                + "  <soap:Header>\n"
                + "    <wsa:Action>" + WsAddressingService.HELLO_ACTION + "</wsa:Action>\n"
                + "    <wsa:MessageID>urn:uuid:12345678-1234-1234-1234-123456789012</wsa:MessageID>\n"
                + "    <wsa:ReplyTo>\n"
                + "      <wsa:Address>http://www.w3.org/2005/08/addressing/anonymous</wsa:Address>\n"
                + "    </wsa:ReplyTo>\n"
                + "    <wsa:To>" + getServerUrl() + "/soapservice/ws-addressing</wsa:To>\n"
                + "  </soap:Header>\n"
                + "  <soap:Body>\n"
                + "    <ns:helloRequest>World</ns:helloRequest>\n"
                + "  </soap:Body>\n"
                + "</soap:Envelope>";

        given()
                .header("Content-Type", "text/xml")
                .body(soapRequest)
                .when()
                .post("/soapservice/ws-addressing")
                .then()
                .statusCode(200)
                .body(CoreMatchers.containsString("Hello World from WS-Addressing service"));
    }

    @Test
    public void wsAddressingWithoutHeaders() {
        final String soapRequest = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\""
                + " xmlns:ns=\"" + WsAddressingService.TARGET_NS + "\">\n"
                + "  <soap:Header/>\n"
                + "  <soap:Body>\n"
                + "    <ns:helloRequest>World</ns:helloRequest>\n"
                + "  </soap:Body>\n"
                + "</soap:Envelope>";

        given()
                .header("Content-Type", "text/xml")
                .body(soapRequest)
                .when()
                .post("/soapservice/ws-addressing")
                .then()
                .statusCode(500)
                .body(CoreMatchers
                        .containsString("A required header representing a Message Addressing Property is not present"));
    }

    @Test
    public void wsAddressingClient() {
        given()
                .body("CamelClient")
                .when()
                .post("/cxf-soap-server/ws-addressing/client/hello")
                .then()
                .statusCode(200)
                .body(CoreMatchers.containsString("Hello CamelClient from WS-Addressing service"));
    }

}
