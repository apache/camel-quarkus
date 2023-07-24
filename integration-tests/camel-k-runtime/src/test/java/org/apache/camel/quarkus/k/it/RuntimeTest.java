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
package org.apache.camel.quarkus.k.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.path.json.JsonPath;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.quarkus.core.FastCamelContext;
import org.apache.camel.quarkus.k.runtime.Application;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class RuntimeTest {
    @Test
    public void inspect() {
        JsonPath p = given()
                .accept(MediaType.APPLICATION_JSON)
                .get("/camel-k/inspect")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertThat(p.getString("camel-context"))
                .isEqualTo(FastCamelContext.class.getName());
        assertThat(p.getString("camel-k-runtime"))
                .isEqualTo(Application.Runtime.class.getName());
        assertThat(p.getString("routes-collector"))
                .isEqualTo(Application.NoRoutesCollector.class.getName());
    }

    @Test
    public void properties() {
        given().get("/camel-k/property/my-property").then().statusCode(200).body(is("my-test-value"));
        given().get("/camel-k/property/root.key").then().statusCode(200).body(is("root.value"));
        given().get("/camel-k/property/001.key").then().statusCode(200).body(is("001.value"));
        given().get("/camel-k/property/002.key").then().statusCode(200).body(is("002.value"));
        given().get("/camel-k/property/a.key").then().statusCode(200).body(is("a.002"));
        given().get("/camel-k/property/flat-property").then().statusCode(200).body(is("flat-value"));
    }
}
