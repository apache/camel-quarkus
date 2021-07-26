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
package org.apache.camel.quarkus.component.velocity.it;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class VelocityTest {

    public static final String OLD_BODY = "old_body";
    public static final String BODY = "bar";
    public static final String NEW_BODY = "new_body";
    public static final String MSG = "\nDear Sheldon\n" +
            "\n" +
            "Thanks for the order of Camel in Action.\n" +
            "\n" +
            "Regards Apache Camel Riders Bookstore\n" +
            "PS: Next beer is on me";

    //@Test
    public void testTemplateViaFile() throws IOException {
        File template = createFile("velocity_test", "Hi ${headers.name}. You have got ${headers.item}. ${body}");
        RestAssured.given()
                .queryParam("name", "Sheldon")
                .queryParam("item", "Camel in Action")
                .queryParam("template", "file:/" + template.getPath())
                .contentType(ContentType.TEXT)
                .body("PS: Next beer is on me")
                .post("/velocity/template")
                .then()
                .statusCode(201)
                .body(equalTo("Hi Sheldon. You have got Camel in Action. PS: Next beer is on me"));
    }

    //@Test
    public void testTemplateViaClasspath() {
        RestAssured.given()
                .queryParam("name", "Sheldon")
                .queryParam("item", "Camel in Action")
                .queryParam("template", "//template/letter.vm")
                .contentType(ContentType.TEXT)
                .body("PS: Next beer is on me")
                .post("/velocity/template")
                .then()
                .statusCode(201)
                .body(equalTo(MSG));
    }

    //@Test
    public void testTemplateViaClasspathWithProperties() {
        //class loader is forbidden by properties, response should fail
        RestAssured.given()
                .queryParam("name", "Sheldon")
                .queryParam("item", "Camel in Action")
                .queryParam("template", "//template/template.vm")
                .queryParam("propertiesFile", "/template/velocity.properties")
                .queryParam("expectFailure", "true")
                .contentType(ContentType.TEXT)
                .body("PS: Next beer is on me")
                .post("/velocity/template")
                .then()
                .statusCode(500)
                .body(containsString("Exception"));
    }

    //@Test
    public void testTemplateViaHeader() {
        RestAssured.given()
                .queryParam("body", "PS: Next beer is on me.")
                .queryParam("name", "Sheldon")
                .queryParam("item", "Camel in Action")
                .contentType(ContentType.TEXT)
                .body("Hi ${headers.name}. Thanks for ${headers.item}. ${body}")
                .post("/velocity/templateViaHeader")
                .then()
                .statusCode(201)
                .body(equalTo("Hi Sheldon. Thanks for Camel in Action. PS: Next beer is on me."));
    }

    //@Test
    public void testSupplementalContext() {
        final String template = "#set( $headers.body = ${body} )\n" + BODY;
        Map result = RestAssured.given()
                .queryParam("body", OLD_BODY)
                .queryParam("supplementalBody", NEW_BODY)
                .contentType(ContentType.TEXT)
                .body(template)
                .post("/velocity/supplementalContext")
                .then()
                .statusCode(200)
                .extract().as(Map.class);

        assertTrue(result.containsKey("result_value"));
        assertEquals(BODY, result.get("result_value"));
        assertTrue(result.containsKey("body"));
        assertEquals(NEW_BODY, result.get("body"));
    }

    //@Test
    public void testBodyAsDomainObject() {
        RestAssured.given()
                .queryParam("name", "Sheldon")
                .queryParam("country", "Earth 1")
                .contentType(ContentType.JSON)
                .body(new Person("Sheldon", "Earth 2"))
                .post("/velocity/bodyAsDomainObject")
                .then()
                .statusCode(201)
                .body(equalTo("\nHi Sheldon from Earth 2"));
    }

    //@Test
    public void testContentCacheFalse() throws Exception {
        testContentCache(false);
    }

    //@Test
    public void testContentCacheTrue() throws Exception {
        testContentCache(true);
    }

    private void testContentCache(boolean useContentCache) throws Exception {
        File template = createFile("velocity_test", "Hi ${body}");

        RestAssured.given()
                .queryParam("template", "file:/" + template.getPath())
                .queryParam("contentCache", useContentCache)
                .contentType(ContentType.TEXT)
                .body("Sheldon")
                .post("/velocity/template")
                .then()
                .statusCode(201)
                .body(equalTo("Hi Sheldon"));

        //override file
        Files.write(Paths.get(template.getPath()), "Bye ${body}".getBytes(StandardCharsets.UTF_8));

        RestAssured.given()
                .queryParam("template", "file:/" + template.getPath())
                .queryParam("contentCache", useContentCache)
                .contentType(ContentType.TEXT)
                .body("Sheldon")
                .post("/velocity/template")
                .then()

                .statusCode(201)
                .body(equalTo(useContentCache ? "Hi Sheldon" : "Bye Sheldon"));
    }

    private File createFile(String fileName, String body) throws IOException {
        File tmpFile = File.createTempFile(fileName, ".vm");

        Files.write(tmpFile.toPath(), body.getBytes(StandardCharsets.UTF_8));

        tmpFile.deleteOnExit();
        return tmpFile;
    }
}
