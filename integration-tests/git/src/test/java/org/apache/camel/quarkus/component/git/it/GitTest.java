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
package org.apache.camel.quarkus.component.git.it;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;

@QuarkusTest
class GitTest {

    //@Test
    void initAddCommit() {
        final String repoName = "testRepo-" + UUID.randomUUID().toString();
        final String path = RestAssured.given()
                .contentType(ContentType.TEXT)
                .post("/git/init/" + repoName)
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();
        final Path repoPath = Paths.get(path);

        assertThat(repoPath).exists();
        assertThat(repoPath.resolve(".git")).exists();

        final String file = "foo/bar.txt";
        final String fileContent = "foo/bar";
        RestAssured.given()
                .contentType(ContentType.BINARY)
                .body(fileContent.getBytes(StandardCharsets.UTF_8))
                .post("/git/add-and-commit/" + repoName + "/" + file)
                .then()
                .statusCode(201)
                .body(is("target/" + repoName + "/" + file));

        assertThat(repoPath).exists();
        assertThat(repoPath.resolve(file)).exists().hasContent(fileContent);

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .get("/git/log/" + repoName)
                .then()
                .statusCode(200)
                .body(containsString("Add " + file));

    }

}
