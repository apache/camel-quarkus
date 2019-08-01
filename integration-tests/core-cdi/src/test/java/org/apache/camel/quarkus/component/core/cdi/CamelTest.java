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
package org.apache.camel.quarkus.component.core.cdi;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class CamelTest {
    @Test
    public void testRoutes() {
        RestAssured.when().get("/test/routes").then().body(containsString("hello"));
    }

    @Test
    public void testProperties() {
        RestAssured.when().get("/test/property/initializing").then().body(is("true"));
        RestAssured.when().get("/test/property/started").then().body(is("true"));
    }

    @Test
    public void testHello() {
        RestAssured.when().get("/test/hello/quarkus").then().body(is("hello quarkus"));
    }
}
