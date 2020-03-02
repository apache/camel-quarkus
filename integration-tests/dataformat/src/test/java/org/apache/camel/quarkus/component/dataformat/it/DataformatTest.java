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
package org.apache.camel.quarkus.component.dataformat.it;

import java.util.stream.Stream;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.CoreMatchers.equalTo;

@QuarkusTest
class DataformatTest {

    private static Stream<String> snakeyamlRoutes() {
        return Stream.of("dataformat-component", "dsl");
    }

    @ParameterizedTest
    @MethodSource("snakeyamlRoutes")
    public void snakeYaml(String route) {
        RestAssured.get("/dataformat/snakeyaml/marshall/" + route + "?name=Camel SnakeYAML")
                .then()
                .statusCode(200)
                .body(equalTo("!!org.apache.camel.quarkus.component.dataformat.it.model.TestPojo {name: Camel SnakeYAML}\n"));

        RestAssured
                .given()
                .contentType("text/yaml")
                .body("!!org.apache.camel.quarkus.component.dataformat.it.model.TestPojo {name: Camel SnakeYAML}")
                .post("/dataformat/snakeyaml/unmarshall/" + route)
                .then()
                .statusCode(200)
                .body(equalTo("Camel SnakeYAML"));
    }

}
