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

import java.util.List;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.hamcrest.Matchers.containsString;

public class SwaggerCamelUrlTest {
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-swagger-ui", Version.getVersion())))
            .overrideConfigKey("camel.rest.apiContextPath", "/camel/openapi")
            .overrideConfigKey("quarkus.swagger-ui.urls-primary-name", "camel")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(RestRoutes.class, QuarkusResource.class)
                    .addAsResource("routes/rests.xml", "routes/rests.xml")
                    .addAsResource("routes/routes.xml", "routes/routes.xml"));

    @Test
    void swaggerUiContainsCamelApiContextPath() {
        RestAssured.given()
                .get("/q/swagger-ui")
                .then()
                .statusCode(200)
                .body(containsString(" url: '/camel/openapi'"));
    }
}
