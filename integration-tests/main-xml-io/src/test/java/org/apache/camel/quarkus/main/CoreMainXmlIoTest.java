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
package org.apache.camel.quarkus.main;

import java.util.List;

import javax.ws.rs.core.MediaType;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.apache.camel.dsl.xml.io.XmlRoutesBuilderLoader;
import org.apache.camel.quarkus.core.DisabledModelJAXBContextFactory;
import org.apache.camel.quarkus.core.DisabledModelToXMLDumper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class CoreMainXmlIoTest {
    @Test
    public void testMainInstanceWithXmlRoutes() {
        JsonPath p = RestAssured.given()
                .accept(MediaType.APPLICATION_JSON)
                .get("/xml-io/describe")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertThat(p.getString("xml-model-dumper")).isEqualTo(DisabledModelToXMLDumper.class.getName());
        assertThat(p.getString("xml-model-factory")).isEqualTo(DisabledModelJAXBContextFactory.class.getName());

        assertThat(p.getString("xml-routes-builder-loader"))
                .isEqualTo(XmlRoutesBuilderLoader.class.getName());

        assertThat(p.getList("routeBuilders", String.class))
                .contains("org.apache.camel.quarkus.main.XmlIoRoutes");

        List<String> routes = p.getList("routes", String.class);
        assertThat(routes)
                .contains("my-xml-route");
        assertThat(routes)
                .contains("templated-route");
        assertThat(routes)
                .contains("rest-route");
    }

    @Test
    public void namespaceAware() {
        String message = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<foo:foo-text xmlns:foo=\"http://camel.apache.org/foo\">bar</foo:foo-text>";

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(message)
                .post("/xml-io/route/namespace-aware")
                .then()
                .statusCode(200)
                .body(is("bar"));

    }

    @Test
    public void validate() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Kermit")
                .post("/xml-io/route/validate")
                .then()
                .statusCode(200)
                .body(Matchers.is("Hello Kermit you were validated"));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Cookie Monster") // does not match ^K.*
                .post("/xml-io/route/validate")
                .then()
                .statusCode(500);

    }

    @Test
    public void routeEncodedInIso8859_15_ShouldSucceed() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("in")
                .post("/xml-io/route/iso_8859_15-encoded")
                .then()
                .statusCode(200)
                .body(Matchers.is("Hello World from ISO-8859-15 encoded route containing € symbol !"));
    }

}
