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
import org.apache.camel.quarkus.core.DisabledXMLRoutesDefinitionLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class CoreMainXmlIoTest {
    //@Test
    public void testMainInstanceWithXmlRoutes() {
        JsonPath p = RestAssured.given()
                .accept(MediaType.APPLICATION_JSON)
                .get("/test/main/describe")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertThat(p.getString("xml-model-dumper")).isEqualTo(DisabledModelToXMLDumper.class.getName());
        assertThat(p.getString("xml-model-factory")).isEqualTo(DisabledModelJAXBContextFactory.class.getName());

        assertThat(p.getString("xml-routes-definitions-loader"))
                .isEqualTo(DisabledXMLRoutesDefinitionLoader.class.getName());
        assertThat(p.getString("xml-routes-builder-loader"))
                .isEqualTo(XmlRoutesBuilderLoader.class.getName());

        assertThat(p.getList("routeBuilders", String.class))
                .isEmpty();

        List<String> routes = p.getList("routes", String.class);
        assertThat(routes)
                .contains("my-xml-route");
        assertThat(routes)
                .contains("templated-route");
        assertThat(routes)
                .contains("rest-route");
    }

    //@Test
    public void namespaceAware() {
        String message = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<foo:foo-text xmlns:foo=\"http://camel.apache.org/foo\">bar</foo:foo-text>";

        RestAssured.given()
                .contentType(ContentType.XML)
                .body(message)
                .post("/test/xml-io/namespace-aware")
                .then()
                .statusCode(200)
                .body(is("bar"));

    }

}
