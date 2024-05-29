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
package org.apache.camel.quarkus.component.graphql.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
class GraphQLTest {

    @Test
    public void testGraphQLQueryFile() {
        RestAssured
                .given()
                .queryParam("testPort", RestAssured.port)
                .queryParam("bookId", "1")
                .get("/graphql/queryFile")
                .then()
                .statusCode(200)
                .body(
                        "data.bookById.id", is(1),
                        "data.bookById.name", is("Harry Potter and the Philosophers Stone"),
                        "data.bookById.author", is("J.K Rowling"));
    }

    @Test
    public void testGraphQLMutation() {
        int id = 4;
        String bookName = "The Great Gatsby";
        String author = "F. Scott Fitzgerald";

        RestAssured
                .given()
                .queryParam("testPort", RestAssured.port)
                .queryParam("bookId", id)
                .queryParam("name", bookName)
                .queryParam("author", author)
                .post("/graphql/mutation")
                .then()
                .statusCode(200)
                .body(
                        "data.addBook.name", is(bookName),
                        "data.addBook.author", is(author),
                        "data.addBook.id", is(id));

        RestAssured
                .given()
                .queryParam("testPort", RestAssured.port)
                .queryParam("bookId", id)
                .get("/graphql/queryFile")
                .then()
                .statusCode(200)
                .body(
                        "data.bookById.name", is(bookName),
                        "data.bookById.author", is(author),
                        "data.bookById.id", is(id));
    }

    @Test
    public void testInlineQuery() {
        RestAssured
                .given()
                .queryParam("testPort", RestAssured.port)
                .get("/graphql/query")
                .then()
                .statusCode(200)
                .body(
                        "data.books[0].id", is(1),
                        "data.books[0].name", is("Harry Potter and the Philosophers Stone"),
                        "data.books[0].author", nullValue(),
                        "data.books[1].id", is(2),
                        "data.books[1].name", is("Moby Dick"),
                        "data.books[1].author", nullValue(),
                        "data.books[2].id", is(3),
                        "data.books[2].name", is("Interview with the vampire"),
                        "data.books[2].author", nullValue());
    }

    @Test
    public void testQuerywithVariables() {
        RestAssured
                .given()
                .queryParam("testPort", RestAssured.port)
                .get("/graphql/queryVariables")
                .then()
                .statusCode(200)
                .body(
                        "data.bookById.id", is(1),
                        "data.bookById.name", is("Harry Potter and the Philosophers Stone"),
                        "data.bookById.author", is("J.K Rowling"));
    }
}
