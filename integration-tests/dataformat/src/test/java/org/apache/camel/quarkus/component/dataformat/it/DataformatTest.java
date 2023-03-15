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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.CoreMatchers.equalTo;

@Disabled("https://github.com/apache/camel-quarkus/issues/4662")
@QuarkusTest
class DataformatTest {

    private static Stream<String> snakeyamlRoutes() {
        return Stream.of("dataformat-component", "dsl");
    }

    @ParameterizedTest
    @MethodSource("snakeyamlRoutes")
    public void snakeYaml(String route) {
        RestAssured.get("/dataformat/snakeyaml/marshal/" + route + "?name=Camel SnakeYAML")
                .then()
                .statusCode(200)
                .body(equalTo("!!org.apache.camel.quarkus.component.dataformat.it.model.TestPojo {name: Camel SnakeYAML}\n"));

        RestAssured
                .given()
                .contentType("text/yaml")
                .body("!!org.apache.camel.quarkus.component.dataformat.it.model.TestPojo {name: Camel SnakeYAML}")
                .post("/dataformat/snakeyaml/unmarshal/" + route)
                .then()
                .statusCode(200)
                .body(equalTo("Camel SnakeYAML"));
    }

    @Test
    public void ical() throws ParseException, IOException {
        final ZonedDateTime START = LocalDateTime.of(2007, 12, 3, 10, 15, 30).atZone(ZoneId.systemDefault());
        final ZonedDateTime END = LocalDateTime.of(2007, 12, 03, 11, 16, 31).atZone(ZoneId.systemDefault());

        final String icsTemplate = IOUtils.toString(getClass().getResourceAsStream("/test.ics"), StandardCharsets.UTF_8);
        final String icalString = String.format(
                icsTemplate,
                toFormatedLocalDateTime(START),
                toFormatedLocalDateTime(END),
                START.getZone().getId());

        final String actualIcal = RestAssured
                .given()
                .queryParam("start", START.toString())
                .queryParam("end", END.toString())
                .queryParam("summary", "Progress Meeting")
                .queryParam("attendee", "dev1@mycompany")
                .get("/dataformat/ical/marshal")
                .then()
                .statusCode(200)
                .extract().body().asString();
        Assertions.assertEquals(icalString, actualIcal.replace("\r", ""));

        final String body = RestAssured
                .given()
                .contentType("text/calendar")
                .body(icalString)
                .post("/dataformat/ical/unmarshal")
                .then()
                .statusCode(200)
                .extract().body().asString();
        Assertions.assertEquals(icalString, body.replace("\r", ""));
    }

    static String toFormatedLocalDateTime(ZonedDateTime zonedDateTime) {
        String result = zonedDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'hhmmss"));
        if (zonedDateTime.getZone().getId().equals("Etc/UTC")) {
            result += "Z";
        }
        return result;
    }

}
