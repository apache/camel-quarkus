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
package org.apache.camel.quarkus.component.language.it;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.test.wiremock.MockServer;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.request;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
@QuarkusTestResource(LanguageTestResource.class)
class LanguageTest {

    @MockServer
    WireMockServer server;

    /**
     * Map endpoint names to objects containing exchange bodies and expected output
     */
    private static Map<String, TestResult> languageResultsMap = Map.ofEntries(
            /* Test Languages */
            Map.entry("languageBeanScript", new TestResult("Hello Bean", "HELLO BEAN")),
            Map.entry("languageConstantScript", new TestResult("Constant", "Hello from constant language script")),
            Map.entry("languageExchangePropertyScript",
                    new TestResult("ExProperty", "Hello ExProperty from exchangeProperty language script")),
            Map.entry("languageFileScript", new TestResult("", "File name is test-file.txt")),
            Map.entry("languageHeaderScript", new TestResult("Header", "Hello Header from header language script")),
            Map.entry("languageHl7terserScript", new TestResult(createHl7Message(), "Patient's surname is Smith")),
            Map.entry("languageJsonPathScript",
                    new TestResult("{\"message\": \"Hello from jsonpath\", \"user\": \"camel-quarkus\"}",
                            "Hello from jsonpath")),
            Map.entry("languageRefScript", new TestResult("Hello from Ref", "hello from ref")),
            Map.entry("languageSimpleScript", new TestResult("Simple", "Hello Simple from simple language script")),
            Map.entry("languageTokenizeScript", new TestResult("Hello,Tokenize", "Hello")),
            Map.entry("languageXpathScript", new TestResult("<message>Hello from Xpath</message>", "Hello from Xpath")),
            Map.entry("languageXqueryScript", new TestResult("<message>Hello from XQuery</message>", "HELLO FROM XQUERY")),

            /* Test Resource Loading */
            Map.entry("languageSimpleResource", new TestResult("SimpleRes", "Hello SimpleRes from simple language resource")),
            Map.entry("languageSimpleFile", new TestResult("SimpleFile", "Hello SimpleFile from simple language file")),

            /* Test transform=false option */
            Map.entry("languageSimpleTransform", new TestResult("SimpleTransform", "SimpleTransform"))

    );

    private static Collection<Object[]> getLanguageResultsMap() {
        return languageResultsMap.entrySet()
                .stream()
                .map((entry) -> new Object[] { entry.getKey(), entry.getValue() })
                .collect(Collectors.toList());
    }

    private static final String OUTPUT_DIRECTORY = "target";
    private static final String TEST_FILE = "test-file.txt";
    private static final String SIMPLE_FILE = "hello.simple-file.txt";

    @BeforeAll
    public static void setupTestFiles() throws Exception {
        Files.createDirectories(Paths.get(OUTPUT_DIRECTORY));
        // Write the file used in the 'file' language test
        Files.writeString(Paths.get(OUTPUT_DIRECTORY, TEST_FILE), "Dummy text", StandardCharsets.UTF_8);

        // Copy the simple script from resources to the OUTPUT_DIRECTORY to test reading a script from a file: resource
        Files.copy(
                LanguageTest.class.getClassLoader().getResourceAsStream(SIMPLE_FILE),
                Paths.get(OUTPUT_DIRECTORY, SIMPLE_FILE),
                StandardCopyOption.REPLACE_EXISTING);
    }

    @AfterAll
    public static void deleteTestFiles() throws Exception {
        Files.delete(Paths.get(OUTPUT_DIRECTORY, TEST_FILE));
        Files.delete(Paths.get(OUTPUT_DIRECTORY, SIMPLE_FILE));
    }

    /**
     * This test is called for each entry in {@link languageResultsMap}.
     * The body text is passed to the routeName endpoint, which is checked for successful completion
     * and that the returned body matches the expected result from {@link TestResult}
     * 
     * @param routeName - The name of the endpoint exposed in {@link LanguageResource}
     * @param result    - The {@link TestResource} containing input body and expected result
     */
    @ParameterizedTest
    @MethodSource("getLanguageResultsMap")
    public void testLanguage(String routeName, TestResult result) {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(result.body)
                .post("/language/route/" + routeName)
                .then()
                .statusCode(200)
                .body(Matchers.is(result.expected));
    }

    /**
     * Tests whether scripts can be loaded over HTTP.
     * 
     * @throws Exception
     */
    @Test
    public void testHttpResource() throws Exception {
        // Stub the WireMock server to return a simple script on the "/simple" endpoint
        assertNotNull(server);
        server.stubFor(request("GET", urlPathEqualTo("/simple"))
                .willReturn(aResponse().withBody("Hello ${body} from simple language http")));

        // The heart of the test is very similar to the testLanguage method above, but pass in the 
        // URL of the WireMock server as a query parameter.
        RestAssured.given()
                .queryParam("baseUrl", server.baseUrl())
                .contentType(ContentType.TEXT)
                .body("SimpleHttp")
                .post("/language/route/languageSimpleHttp")
                .then()
                .statusCode(200)
                .body(Matchers.is("Hello SimpleHttp from simple language http"));
        server.verify(1, getRequestedFor(urlEqualTo("/simple")));
    }

    /**
     * Tests the contentCache option. The WireMock server is used to count how many times
     * the script endpoint was accessed during route execution.
     */
    @Test
    public void testContentCache() {
        // Create a WireMock stub to return a simple script
        assertNotNull(server);
        server.stubFor(request("GET", urlPathEqualTo("/simpleContentCache"))
                .willReturn(aResponse().withBody("Hello ${body} from simple language http")));

        // Set up common request options
        RequestSpecBuilder builder = new RequestSpecBuilder();
        builder.addQueryParam("baseUrl", server.baseUrl());
        builder.setContentType(ContentType.TEXT);
        builder.setBody("SimpleHttp");

        // Call the route to load the script with caching enabled
        RestAssured.given().spec(builder.build())
                .queryParam("contentCache", true)
                .when().post("/language/route/languageSimpleContentCache")
                .then()
                .statusCode(200)
                .body(Matchers.is("Hello SimpleHttp from simple language http"));

        // As this is the first time the route has been called, expect the script to have been loaded
        server.verify(1, getRequestedFor(urlEqualTo("/simpleContentCache")));

        // Call the endpoint again with caching enabled
        RestAssured.given().spec(builder.build())
                .queryParam("contentCache", true)
                .when().post("/language/route/languageSimpleContentCache")
                .then()
                .statusCode(200);

        // Expect the script to have been cached, so the request count should not have changed
        server.verify(1, getRequestedFor(urlEqualTo("/simpleContentCache")));

        // Call the route a third time without caching
        RestAssured.given().spec(builder.build())
                .queryParam("contentCache", false)
                .when().post("/language/route/languageSimpleContentCache")
                .then()
                .statusCode(200);

        // Expect the script to have been loaded a second time
        server.verify(2, getRequestedFor(urlEqualTo("/simpleContentCache")));

    }

    /**
     * Creates a health record for a fictional patient.
     * 
     * @return
     */
    private static String createHl7Message() {
        return "MSH|^~\\&|||||20230221140012.386+0000||ADT^A01^ADT_A01|601|P|2.4\r"
                + "PID|||1||Smith^John";
    }

    private static class TestResult {
        String body;
        String expected;

        public TestResult(String body, String expected) {
            this.body = body;
            this.expected = expected;
        }
    }

}
