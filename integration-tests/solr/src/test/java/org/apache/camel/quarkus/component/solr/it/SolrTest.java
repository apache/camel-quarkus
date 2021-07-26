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
package org.apache.camel.quarkus.component.solr.it;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@QuarkusTestResource(SolrTestResource.class)
public class SolrTest {
    /**
     * Quarkus resources to test
     *
     * @return
     */
    private static Stream<String> resources() {
        return Stream.of("/solr/standalone", "/solr/ssl", "/solr/cloud");
    }

    //@ParameterizedTest
    @MethodSource("resources")
    public void testSingleBean(String resource) {
        // create a bean item
        given()
                .contentType(ContentType.JSON)
                .body("test1")
                .put(resource + "/bean")
                .then()
                .statusCode(202);
        // verify existing bean
        given()
                .get(resource + "/bean/test1")
                .then()
                .body(equalTo("test1"));

        // delete bean by id
        given()
                .contentType(ContentType.JSON)
                .body("test1")
                .delete(resource + "/bean")
                .then()
                .statusCode(202);
        // verify non existing bean
        given()
                .get(resource + "/bean/test1")
                .then()
                .body(emptyOrNullString());
    }

    //@ParameterizedTest
    @MethodSource("resources")
    public void testMultipleBeans(String resource) {
        // create list of beans
        List<String> beanNames = new ArrayList<>();
        beanNames.add("bean1");
        beanNames.add("bean2");

        // add beans with camel
        given()
                .contentType(ContentType.JSON)
                .body(beanNames)
                .put(resource + "/beans")
                .then()
                .statusCode(202);

        // verify existing beans
        given()
                .get(resource + "/bean/bean1")
                .then()
                .body(equalTo("bean1"));
        given()
                .get(resource + "/bean/bean2")
                .then()
                .body(equalTo("bean2"));

        // delete all beans that has id begins with bean
        given()
                .contentType(ContentType.JSON)
                .body("bean")
                .delete(resource + "/beans")
                .then()
                .statusCode(202);

        // verify non existing beans
        given()
                .get(resource + "/bean/bean1")
                .then()
                .body(emptyOrNullString());
        given()
                .get(resource + "/bean/bean2")
                .then()
                .body(emptyOrNullString());
    }

    //@ParameterizedTest
    @MethodSource("resources")
    public void testInsertId(String resource) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("id", "id1");

        //insert and commit document
        given()
                .contentType(ContentType.JSON)
                .body(fields)
                .put(resource + "/document/commit")
                .then()
                .statusCode(202);

        // verify existing document
        given()
                .get(resource + "/bean/id1")
                .then()
                .body(equalTo("id1"));

    }

    //@ParameterizedTest
    @MethodSource("resources")
    public void testOptimize(String resource) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("id", "opt1");
        // insert without commit
        given()
                .contentType(ContentType.JSON)
                .body(fields)
                .put(resource + "/document")
                .then()
                .statusCode(202);
        // verify non existing document
        given()
                .get(resource + "/bean/opt1")
                .then()
                .body(emptyOrNullString());
        // optimize
        given()
                .get(resource + "/optimize")
                .then()
                .statusCode(202);
        // verify existing document
        given()
                .get(resource + "/bean/opt1")
                .then()
                .body(equalTo("opt1"));

    }

    //  Rollback is currently not supported in SolrCloud mode (SOLR-4895). So limiting this test to standalone and standalone with SSL modes
    //@ParameterizedTest
    @ValueSource(strings = { "/solr/standalone", "/solr/ssl" })
    public void testRollback(String resource) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("id", "roll1");
        // insert without commit
        given()
                .contentType(ContentType.JSON)
                .body(fields)
                .put(resource + "/document")
                .then()
                .statusCode(202);
        // verify non existing document
        given()
                .get(resource + "/bean/roll1")
                .then()
                .body(emptyOrNullString());
        // rollback
        given()
                .get(resource + "/rollback")
                .then()
                .statusCode(202);
        //then commit
        given()
                .get(resource + "/commit")
                .then()
                .statusCode(202);
        // verify non existing document
        given()
                .get(resource + "/bean/roll1")
                .then()
                .body(emptyOrNullString());
    }

    //@ParameterizedTest
    @MethodSource("resources")
    public void testSoftCommit(String resource) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("id", "com1");
        // insert without commit
        given()
                .contentType(ContentType.JSON)
                .body(fields)
                .put(resource + "/document")
                .then()
                .statusCode(202);
        // verify non existing document
        given()
                .get(resource + "/bean/com1")
                .then()
                .body(emptyOrNullString());
        // soft commit
        given()
                .get(resource + "/softcommit")
                .then()
                .statusCode(202);
        // verify existing document
        given()
                .get(resource + "/bean/com1")
                .then()
                .body(equalTo("com1"));
    }

    //@ParameterizedTest
    @MethodSource("resources")
    public void testInsertStreaming(String resource) throws InterruptedException {
        Map<String, Object> fields = new HashMap<>();
        fields.put("id", "stream1");
        // insert with streaming mode
        given()
                .contentType(ContentType.JSON)
                .body(fields)
                .put(resource + "/streaming")
                .then()
                .statusCode(202);
        // wait before commit
        Thread.sleep(500);
        given()
                .get(resource + "/commit")
                .then()
                .statusCode(202);
        // verify existing document
        given()
                .get(resource + "/bean/stream1")
                .then()
                .body(equalTo("stream1"));
    }
}
