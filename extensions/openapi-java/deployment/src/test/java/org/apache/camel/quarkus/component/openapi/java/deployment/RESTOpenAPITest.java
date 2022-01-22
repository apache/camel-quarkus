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

package org.apache.camel.quarkus.component.openapi.java.deployment;

import java.io.File;
import java.util.Arrays;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.builder.Version;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.hamcrest.Matchers.is;

public class RESTOpenAPITest {
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setForcedDependencies(Arrays.asList(
                    new AppArtifact("io.quarkus", "quarkus-smallrye-openapi", Version.getVersion())))
            .withConfigurationResource("application.properties")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(RestRoutes.class, QuarkusResource.class)
                    .addAsResource(new File("src/test/resources/routes/my-route.xml"), "routes/my-route.xml"));

    @BeforeAll
    static void setUp() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    void test() {
        RestAssured.given()
                .get("/q/openapi?format=json")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("servers[0].url", is("http://localhost:8080/"),
                        "paths.'/quarkus/camel/api'.get.operationId", is("get"),
                        "paths.'/quarkus/camel/api'.get.summary", is("get test"),
                        "paths.'/quarkus/camel/xml'.get.operationId", is("camel_xml_rest"),
                        "paths.'/quarkus/hello'.get.tags[0]", is("Quarkus Resource"));

        RestAssured.given()
                .get("/camel/api")
                .then()
                .statusCode(200)
                .contentType(ContentType.TEXT)
                .body(is("GET: /rest"));

        RestAssured.given()
                .get("/camel/xml")
                .then()
                .statusCode(200)
                .body(is("Camel XML Rest"));

        RestAssured.given()
                .get("/hello")
                .then()
                .statusCode(200)
                .contentType(ContentType.TEXT)
                .body(is("Hello Quarkus"));
    }
}
