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
package org.apache.camel.quarkus.component.caffeine.it;

import java.util.Locale;
import java.util.UUID;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.Matchers.is;

@QuarkusTest
class CaffeineTest {
    //@Test
    public void loadComponentCaffeineCache() {
        RestAssured.get("/caffeine/component/{componentName}", "caffeine-cache")
                .then()
                .statusCode(200);
        RestAssured.get("/caffeine/component/{componentName}", "caffeine-loadcache")
                .then()
                .statusCode(200);
    }

    //@ParameterizedTest
    @ValueSource(strings = { "embedded", CaffeineCaches.SHARED_CACHE_NAME })
    public void putAndGet(String cacheName) {
        final String key = "the-key";
        final String value = UUID.randomUUID().toString();

        RestAssured.given()
                .body(value)
                .post("/caffeine/request/caffeine-cache/{cacheName}/{key}", cacheName, key)
                .then()
                .statusCode(200);
        RestAssured.get("/caffeine/request/caffeine-cache/{cacheName}/{key}", cacheName, key)
                .then()
                .statusCode(200)
                .body(is(value));
    }

    //@Test
    public void valueIsLoadedByTheCache() {
        final String key = "the-key";
        final String value = "the-key";

        RestAssured.get("/caffeine/request/caffeine-loadcache/{cacheName}/{key}", CaffeineCaches.LOADING_CACHE_NAME, key)
                .then()
                .statusCode(200)
                .body(is(value.toUpperCase(Locale.US)));
    }
}
