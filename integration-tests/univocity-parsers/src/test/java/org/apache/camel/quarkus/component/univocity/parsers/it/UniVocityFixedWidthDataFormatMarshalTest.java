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
package org.apache.camel.quarkus.component.univocity.parsers.it;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.apache.camel.quarkus.component.univocity.parsers.it.UniVocityTestHelper.asMap;
import static org.apache.camel.quarkus.component.univocity.parsers.it.UniVocityTestHelper.join;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class UniVocityFixedWidthDataFormatMarshalTest {

    @Test
    void shouldMarshalWithDefaultConfiguration() {
        List<Map<String, String>> object = Arrays.asList(
                asMap("A", "1", "B", "2", "C", "3"),
                asMap("A", "one", "B", "two", "C", "three"));
        String expected = join("1  2  3    ", "onetwothree");
        String url = "/univocity-parsers/marshal/fixed-width/default";
        given().when().contentType(ContentType.JSON).body(object).get(url).then().statusCode(200).body(is(expected));
    }

    @Test
    void shouldMarshalSingleLine() {
        List<Map<String, String>> object = Arrays.asList(
                asMap("A", "1", "B", "2", "C", "3"));
        String expected = join("1  2  3    ");
        String url = "/univocity-parsers/marshal/fixed-width/default";
        given().when().contentType(ContentType.JSON).body(object).get(url).then().statusCode(200).body(is(expected));
    }

    @Test
    void shouldMarshalAndAddNewColumns() {
        List<Map<String, String>> object = Arrays.asList(
                asMap("A", "1", "B", "2"),
                asMap("C", "three", "A", "one", "B", "two"));
        String expected = join("1  2  ", "onetwothree");
        String url = "/univocity-parsers/marshal/fixed-width/default";
        given().when().contentType(ContentType.JSON).body(object).get(url).then().statusCode(200).body(is(expected));
    }

    @Test
    void shouldMarshalWithSpecificHeaders() {
        List<Map<String, String>> object = Arrays.asList(
                asMap("A", "1", "B", "2", "C", "3"),
                asMap("A", "one", "B", "two", "C", "three"));
        String expected = join("1  3    ", "onethree");
        String url = "/univocity-parsers/marshal/fixed-width/header";
        given().when().contentType(ContentType.JSON).body(object).get(url).then().statusCode(200).body(is(expected));
    }

    @Test
    void shouldMarshalUsingAdvancedConfiguration() {
        List<Map<String, String>> object = Arrays.asList(
                asMap("A", null, "B", ""),
                asMap("A", "one", "B", "two"));
        String expected = join("N/A__empty", "one__two__");
        String url = "/univocity-parsers/marshal/fixed-width/advanced";
        given().when().contentType(ContentType.JSON).body(object).get(url).then().statusCode(200).body(is(expected));
    }
}
