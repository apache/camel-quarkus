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

import io.quarkus.test.junit.QuarkusTest;

import static io.restassured.RestAssured.given;
import static org.apache.camel.quarkus.component.univocity.parsers.it.UniVocityTestHelper.asMap;
import static org.apache.camel.quarkus.component.univocity.parsers.it.UniVocityTestHelper.join;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class UniVocityTsvDataFormatUnmarshalTest {

    //@Test
    void shouldUnmarshalWithDefaultConfiguration() {
        String content = join("A\tB\tC", "1\t2\t3", "one\ttwo\tthree");
        String url = "/univocity-parsers/unmarshal/tsv/default";
        List<?> unmarshalleds = given().when().body(content).get(url).then().statusCode(200).extract().as(List.class);
        assertEquals(3, unmarshalleds.size());
        assertEquals(Arrays.asList("A", "B", "C"), unmarshalleds.get(0));
        assertEquals(Arrays.asList("1", "2", "3"), unmarshalleds.get(1));
        assertEquals(Arrays.asList("one", "two", "three"), unmarshalleds.get(2));
    }

    //@Test
    void shouldUnmarshalAsMap() {
        String content = join("A\tB\tC", "1\t2\t3", "one\ttwo\tthree");
        String url = "/univocity-parsers/unmarshal/tsv/map";
        List<?> unmarshalleds = given().when().body(content).get(url).then().statusCode(200).extract().as(List.class);
        assertEquals(2, unmarshalleds.size());
        assertEquals(asMap("A", "1", "B", "2", "C", "3"), unmarshalleds.get(0));
        assertEquals(asMap("A", "one", "B", "two", "C", "three"), unmarshalleds.get(1));
    }

    //@Test
    void shouldUnmarshalAsMapWithHeaders() {
        String content = join("1\t2\t3", "one\ttwo\tthree");
        String url = "/univocity-parsers/unmarshal/tsv/mapWithHeaders";
        List<?> unmarshalleds = given().when().body(content).get(url).then().statusCode(200).extract().as(List.class);
        assertEquals(2, unmarshalleds.size());
        assertEquals(asMap("A", "1", "B", "2", "C", "3"), unmarshalleds.get(0));
        assertEquals(asMap("A", "one", "B", "two", "C", "three"), unmarshalleds.get(1));
    }

    //@Test
    void shouldUnmarshalUsingIterator() {
        String content = join("A\tB\tC", "1\t2\t3", "one\ttwo\tthree");
        String url = "/univocity-parsers/unmarshal/tsv/lazy";
        List<?> unmarshalleds = given().when().body(content).get(url).then().statusCode(200).extract().as(List.class);
        assertEquals(3, unmarshalleds.size());
        assertEquals(Arrays.asList("A", "B", "C"), unmarshalleds.get(0));
        assertEquals(Arrays.asList("1", "2", "3"), unmarshalleds.get(1));
        assertEquals(Arrays.asList("one", "two", "three"), unmarshalleds.get(2));
    }

    //@Test
    void shouldUnmarshalUsingAdvancedConfiguration() {
        String content = join("!This is comment", "!This is comment too", "A\tB", "", "  \tD  ");
        String url = "/univocity-parsers/unmarshal/tsv/advanced";
        List<?> unmarshalleds = given().when().body(content).get(url).then().statusCode(200).extract().as(List.class);
        assertEquals(2, unmarshalleds.size());
        assertEquals(Arrays.asList("A", "B"), unmarshalleds.get(0));
        assertEquals(Arrays.asList("N/A", "D  "), unmarshalleds.get(1));
    }
}
