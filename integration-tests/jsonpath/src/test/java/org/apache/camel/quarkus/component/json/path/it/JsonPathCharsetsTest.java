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
package org.apache.camel.quarkus.component.json.path.it;

import java.io.IOException;

import io.quarkus.test.junit.QuarkusTest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class JsonPathCharsetsTest {

    @Test
    public void transformBooksUTF16BEShouldReturnTwoAuthors() throws IOException {
        byte[] body = IOUtils.resourceToByteArray("/booksUTF16BE.json");
        given().body(body)
                .get("/jsonpath/getAuthorsFromJsonStream")
                .then()
                .statusCode(200)
                .body(is("Sayings of the Century-Sword of Honour"));
    }

    @Test
    public void transformBooksUTF16LEShouldReturnTwoAuthors() throws IOException {
        byte[] body = IOUtils.resourceToByteArray("/booksUTF16LE.json");
        given().body(body)
                .get("/jsonpath/getAuthorsFromJsonStream")
                .then().statusCode(200)
                .body(is("Sayings of the Century-Sword of Honour"));
    }

    @Test
    public void transformBooksIso_8859_1_ShouldReturnTwoAuthors() throws IOException {
        byte[] body = IOUtils.resourceToByteArray("/germanbooks-iso-8859-1.json");
        given().queryParam("encoding", "ISO-8859-1")
                .body(body)
                .get("/jsonpath/getAuthorsFromJsonStream")
                .then()
                .statusCode(200)
                .body(is("Joseph und seine Brüder-Götzendämmerung"));
    }

}
