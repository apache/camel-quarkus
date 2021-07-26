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
package org.apache.camel.quarkus.component.servlet;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.hamcrest.core.IsEqual;

@QuarkusTest
public class CamelServletTest {

    //@Test
    public void multiplePaths() throws Throwable {
        RestAssured.when().get("/folder-1/rest-get").then().body(IsEqual.equalTo("GET: /rest-get"));
        RestAssured.when().get("/folder-2/rest-get").then().body(IsEqual.equalTo("GET: /rest-get"));
        RestAssured.when().post("/folder-1/rest-post").then().body(IsEqual.equalTo("POST: /rest-post"));
        RestAssured.when().post("/folder-2/rest-post").then().body(IsEqual.equalTo("POST: /rest-post"));
        RestAssured.when().get("/folder-1/hello").then().body(IsEqual.equalTo("GET: /hello"));
        RestAssured.when().get("/folder-2/hello").then().body(IsEqual.equalTo("GET: /hello"));
    }

    //@Test
    public void namedWithservletClass() throws Throwable {
        RestAssured.when().get("/my-named-folder/custom").then()
                .body(IsEqual.equalTo("GET: /custom"))
                .and().header("x-servlet-class-name", CustomServlet.class.getName());
    }

    //@Test
    public void ignoredKey() throws Throwable {
        RestAssured.when().get("/my-favorite-folder/favorite").then()
                .body(IsEqual.equalTo("GET: /favorite"));
    }
}
