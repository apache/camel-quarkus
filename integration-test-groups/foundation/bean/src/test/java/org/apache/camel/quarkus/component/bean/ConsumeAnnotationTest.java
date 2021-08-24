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

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
public class ConsumeAnnotationTest {
    @Test
    public void consumeAnnotation() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("foo")
                .post("/bean/route/consumeAnnotation")
                .then()
                .body(equalTo("Consumed foo"));
    }

    @Test
    public void consumeAnnotationWithExplicitProperty() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("bar")
                .post("/bean/route/consumeAnnotationWithExplicitProperty")
                .then()
                .body(equalTo("Consumed bar via direct:consumeAnnotationWithExplicitProperty"));
    }

    @Test
    public void consumeAnnotationWithImplicitGetter() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("baz")
                .post("/bean/route/consumeAnnotationWithImplicitGetter")
                .then()
                .body(equalTo("Consumed baz via direct:consumeAnnotationWithImplicitGetter"));
    }

    @Test
    public void consumeAnnotationWithImplicitEndpointGetter() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("boo")
                .post("/bean/route/consumeAnnotationWithImplicitEndpointGetter")
                .then()
                .body(equalTo("Consumed boo via direct:consumeAnnotationWithImplicitEndpointGetter"));
    }

    @Test
    public void consumeAnnotationWithImplicitOnGetter() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("boo")
                .post("/bean/route/consumeAnnotationWithImplicitOnGetter")
                .then()
                .body(equalTo("Consumed boo via direct:consumeAnnotationWithImplicitOnGetter"));
    }

    @Test
    public void defaultNamedConsumeAnnotation() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("bar")
                .post("/bean/route/defaultNamedConsumeAnnotation")
                .then()
                .body(equalTo("Consumed named bar"));
    }

    @Test
    public void customNamedConsumeAnnotation() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("baz")
                .post("/bean/route/customNamedConsumeAnnotation")
                .then()
                .body(equalTo("Consumed custom named baz"));
    }

    @Test
    public void singletonConsumeAnnotation() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("goo")
                .post("/bean/route/singletonConsumeAnnotation")
                .then()
                .body(equalTo("Consumed singleton goo"));
    }
}
