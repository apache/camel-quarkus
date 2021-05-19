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
package org.apache.camel.quarkus.component.bean;

import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

@QuarkusTest
class EipTest {

    @Test
    public void dynamicRouter() {
        for (int i = 0; i < 4; i++) {
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .body(String.valueOf(i))
                    .post("/bean/eip/route/dynamicRouter")
                    .then()
                    .statusCode(200);
        }

        Awaitility.await().pollInterval(50, TimeUnit.MILLISECONDS).atMost(30, TimeUnit.SECONDS)
                .until(
                        () -> RestAssured.get("/bean/eip/result/dynamicRouterResult0")
                                .then()
                                .statusCode(200)
                                .extract().body().asString(),
                        Matchers.is("0,2"));

        Awaitility.await().pollInterval(50, TimeUnit.MILLISECONDS).atMost(30, TimeUnit.SECONDS)
                .until(
                        () -> RestAssured.get("/bean/eip/result/dynamicRouterResult1")
                                .then()
                                .statusCode(200)
                                .extract().body().asString(),
                        Matchers.is("1,3"));

    }

    @Test
    public void dynamicRouterAnnotation() {
        for (int i = 4; i < 8; i++) {
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .body(String.valueOf(i))
                    .post("/bean/eip/route/dynamicRouterAnnotation")
                    .then()
                    .statusCode(200);
        }

        Awaitility.await().pollInterval(50, TimeUnit.MILLISECONDS).atMost(30, TimeUnit.SECONDS)
                .until(
                        () -> RestAssured.get("/bean/eip/result/dynamicRouterAnnotationResult0")
                                .then()
                                .statusCode(200)
                                .extract().body().asString(),
                        Matchers.is("4,6"));

        Awaitility.await().pollInterval(50, TimeUnit.MILLISECONDS).atMost(30, TimeUnit.SECONDS)
                .until(
                        () -> RestAssured.get("/bean/eip/result/dynamicRouterAnnotationResult1")
                                .then()
                                .statusCode(200)
                                .extract().body().asString(),
                        Matchers.is("5,7"));

    }

}
