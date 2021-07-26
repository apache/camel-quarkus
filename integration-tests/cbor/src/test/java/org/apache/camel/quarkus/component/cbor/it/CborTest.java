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
package org.apache.camel.quarkus.component.cbor.it;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.component.cbor.it.model.Author;
import org.apache.camel.quarkus.component.cbor.it.model.DummyObject;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class CborTest {

    private static final ObjectMapper CBOR_MAPPER = new ObjectMapper(new CBORFactory());

    //@Test
    void marshalUnmarshalMapShouldSucceed() {
        Map<String, String> in = Collections.singletonMap("name", "Camel");

        String uri = "/cbor/marshalUnmarshalMap";
        Map<?, ?> out = given().contentType(ContentType.JSON).body(in).get(uri).then().statusCode(200).extract().as(Map.class);
        assertEquals(1, out.size());
    }

    //@Test
    void marshalUnmarshalAuthorShouldSucceed() {
        Author in = new Author();
        in.setName("Don");
        in.setSurname("Winslow");

        String uri = "/cbor/marshalUnmarshalAuthor";
        Author out = given().contentType(ContentType.JSON).body(in).get(uri).then().statusCode(200).extract().as(Author.class);
        assertEquals("Don", out.getName());
        assertEquals("Winslow", out.getSurname());
    }

    //@Test
    void marshalUnmarshalCborMethod() {
        Author in = new Author();
        in.setName("Joe");
        in.setSurname("Doe");

        String uri = "/cbor/marshalUnmarshalCborMethod";
        Author out = given().contentType(ContentType.JSON).body(in).get(uri).then().statusCode(200).extract().as(Author.class);
        assertEquals("Joe", out.getName());
        assertEquals("Doe", out.getSurname());
    }

    //@Test
    void unmarshalAuthorViaJmsTypeHeaderShouldSucceed() throws JsonProcessingException {
        Author author = new Author();
        author.setName("David");
        author.setSurname("Foster Wallace");

        byte[] authorCborBytes = CBOR_MAPPER.writeValueAsBytes(author);

        String uri = "/cbor/unmarshalAuthorViaJmsTypeHeader";
        Author unmarshalled = given().body(authorCborBytes).get(uri).then().statusCode(200).extract().as(Author.class);
        assertNotNull(unmarshalled);
        assertEquals("David", unmarshalled.getName());
        assertEquals("Foster Wallace", unmarshalled.getSurname());
    }

    //@Test
    void unmarshalDummyObjectListShouldSucceed() throws JsonProcessingException {
        DummyObject first = new DummyObject();
        first.setDummy("value1");
        DummyObject second = new DummyObject();
        second.setDummy("value2");

        byte[] listCborBytes = CBOR_MAPPER.writeValueAsBytes(Arrays.asList(first, second));

        String uri = "/cbor/unmarshalDummyObjectList";
        DummyObject[] objects = given().body(listCborBytes).get(uri).then().statusCode(200).extract().as(DummyObject[].class);
        assertEquals(2, objects.length);
    }

}
