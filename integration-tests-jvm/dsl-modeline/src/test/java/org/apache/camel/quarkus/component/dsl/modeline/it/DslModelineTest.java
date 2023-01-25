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
package org.apache.camel.quarkus.component.dsl.modeline.it;

import java.util.List;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class DslModelineTest {

    @Test
    void testModelineSingleDependency() {
        String line = "// camel-k: dependency=mvn:org.my:application:1.0";
        List<Response> response = parseAndGetDependencies(line);
        assertNotNull(response);
        assertEquals(1, response.size());
        assertTrue(response.contains("mvn:org.my:application:1.0"));
    }

    @Test
    void testModelineSingleDependencyCommentHash() {
        String line = "### camel-k: dependency=mvn:org.my:application:1.0";
        List<Response> response = parseAndGetDependencies(line);
        assertNotNull(response);
        assertEquals(1, response.size());
        assertTrue(response.contains("mvn:org.my:application:1.0"));
    }

    @Test
    void testModelineMultiDependency() {
        String line = "// camel-k: dependency=mvn:org.my:application:1.0 dependency=mvn:com.foo:myapp:2.1";

        List<Response> response = parseAndGetDependencies(line);
        assertNotNull(response);
        assertEquals(2, response.size());
        assertTrue(response.contains("mvn:org.my:application:1.0"));
        assertTrue(response.contains("mvn:com.foo:myapp:2.1"));

    }

    @Test
    void testModelineSingleProperty() throws Exception {
        String line = "// camel-k: property=hi=Hello";
        clear();
        parseModeline(line);

        RestAssured
                .get("/dsl-modeline/props/" + "hi")
                .then()
                .body(equalTo("Hello"));
    }

    @Test
    void testModelineMultiProperty() throws Exception {
        String line = "// camel-k: property=hi=Hello property=bye=Farvel";
        parseModeline(line);

        RestAssured
                .get("/dsl-modeline/props/" + "hi")
                .then()
                .body(equalTo("Hello"));

        RestAssured
                .get("/dsl-modeline/props/" + "bye")
                .then()
                .body(equalTo("Farvel"));

    }

    @Test
    void testModelineQuoteProperty() throws Exception {
        String line = "// camel-k: property=hi='Hello World' property=bye='Farvel Verden'";
        parseModeline(line);

        RestAssured
                .get("/dsl-modeline/props/" + "hi")
                .then()
                .body(equalTo("Hello World"));

        RestAssured
                .get("/dsl-modeline/props/" + "bye")
                .then()
                .body(equalTo("Farvel Verden"));
    }

    @Test
    void testModelineMixed() throws Exception {
        String line = "// camel-k: dependency=mvn:org.my:application:1.0 property=hi=Hello dependency=mvn:com.foo:myapp:2.1";
        List<Response> response = parseAndGetDependencies(line);
        assertNotNull(response);
        assertEquals(2, response.size());
        assertTrue(response.contains("mvn:org.my:application:1.0"));
        assertTrue(response.contains("mvn:com.foo:myapp:2.1"));

        RestAssured
                .get("/dsl-modeline/props/" + "hi")
                .then()
                .body(equalTo("Hello"));
    }

    @Test
    void testModelineMixedWithSpaces() throws Exception {
        String line = "//    camel-k:   dependency=mvn:org.my:application:1.0    property=hi=Hello   dependency=mvn:com.foo:myapp:2.1";
        List<Response> response = parseAndGetDependencies(line);
        assertNotNull(response);
        assertEquals(2, response.size());
        assertTrue(response.contains("mvn:org.my:application:1.0"));
        assertTrue(response.contains("mvn:com.foo:myapp:2.1"));

        RestAssured
                .get("/dsl-modeline/props/" + "hi")
                .then()
                .body(equalTo("Hello"));

    }

    @Test
    void testModelinePropertiesFile() throws Exception {
        String line = "// camel-k: property=classpath:myapp.properties";
        parseModeline(line);

        RestAssured
                .get("/dsl-modeline/props/" + "hi")
                .then()
                .body(equalTo("Hej"));

        RestAssured
                .get("/dsl-modeline/props/" + "foo")
                .then()
                .body(equalTo("bar"));

    }

    private List parseAndGetDependencies(String line) {
        // clear list
        clear();

        //compute new results
        parseModeline(line);

        //get dependencies
        return RestAssured
                .given()
                .contentType(ContentType.JSON)
                .when()
                .body(line)
                .get("/dsl-modeline/deps")
                .then()
                .extract().body().as(List.class);

    }

    private void clear() {
        // clear list
        RestAssured.delete("/dsl-modeline")
                .then()
                .statusCode(200);
    }

    private void parseModeline(String line) {
        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .when()
                .body(line)
                .post("/dsl-modeline")
                .then()
                .statusCode(200);
    }
}
