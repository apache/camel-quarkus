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
public class BeanTest {
    @Test
    public void testRoutes() {
        RestAssured.given().contentType(ContentType.TEXT).body("nuts@bolts").post("/bean/process-order").then()
                .body(equalTo("{success=true, lines=[(id=1,item=nuts), (id=2,item=bolts)]}"));

        /* Ensure that the RoutesBuilder.configure() was not called multiple times on CamelRoute */
        RestAssured.when()
                .get("/bean/camel-configure-counter")
                .then()
                .statusCode(200)
                .body(equalTo("1"));
    }

    @Test
    public void named() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Kermit")
                .post("/bean/named")
                .then()
                .body(equalTo("Hello Kermit from the NamedBean"));
    }

    @Test
    public void method() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Kermit")
                .post("/bean/method")
                .then()
                .body(equalTo("Hello Kermit from the MyBean"));
    }

    @Test
    public void handler() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Kermit")
                .post("/bean/handler")
                .then()
                .body(equalTo("Hello Kermit from the WithHandlerBean"));
    }

    @Test
    public void handlerWithProxy() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Kermit")
                .post("/bean/handlerOnProxy")
                .then()
                .body(equalTo("Hello Kermit from the WithHandlerBean"));
    }

    @Test
    public void inject() {

        /* Ensure that @Inject works */
        RestAssured.when().get("/bean/counter").then().body(equalTo("0"));
        RestAssured.when().get("/bean/route-builder-injected-count").then().body(equalTo("0"));
        RestAssured.when().get("/bean/increment").then().body(equalTo("1"));
        RestAssured.when().get("/bean/counter").then().body(equalTo("1"));
        RestAssured.when().get("/bean/route-builder-injected-count").then().body(equalTo("1"));
        RestAssured.when().get("/bean/increment").then().body(equalTo("2"));
        RestAssured.when().get("/bean/counter").then().body(equalTo("2"));
        RestAssured.when().get("/bean/route-builder-injected-count").then().body(equalTo("2"));

        /* Ensure that @ConfigProperty works */
        RestAssured.when()
                .get("/bean/config-property")
                .then()
                .statusCode(200)
                .body(equalTo("myFooValue = foo"));

        /* Ensure that the bean was not instantiated multiple times */
        RestAssured.when()
                .get("/bean/route-builder-instance-counter")
                .then()
                .statusCode(200)
                .body(equalTo("1"));

        /* Ensure that the RoutesBuilder.configure() was not called multiple times */
        RestAssured.when()
                .get("/bean/route-builder-configure-counter")
                .then()
                .statusCode(200)
                .body(equalTo("1"));
    }

    @Test
    public void lazy() {
        RestAssured.when().get("/bean/lazy").then().body(equalTo("lazy"));
    }

    @Test
    public void withProducer() {
        RestAssured.when().get("/bean/with-producer").then().body(equalTo("with-producer"));
    }

}
