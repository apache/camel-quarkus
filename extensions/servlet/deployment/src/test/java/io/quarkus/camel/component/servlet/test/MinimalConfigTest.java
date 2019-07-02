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
package io.quarkus.camel.component.servlet.test;

import org.apache.camel.builder.RouteBuilder;
import org.hamcrest.core.IsEqual;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class MinimalConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Routes.class)
                    .addAsResource(new StringAsset("quarkus.camel.servlet.url-patterns=/*\n"),
                            "application.properties"));

    @Test
    public void minimal() {
        RestAssured.when().get("/rest-get").then().body(IsEqual.equalTo("GET: /rest-get"));
        RestAssured.when().post("/rest-post").then().body(IsEqual.equalTo("POST: /rest-post"));
        RestAssured.when().get("/hello").then().body(IsEqual.equalTo("GET: /hello"));
    }

    public static class Routes extends RouteBuilder {

        @Override
        public void configure() {

            rest()
                    .get("/rest-get")
                    .route()
                    .setBody(constant("GET: /rest-get"))
                    .endRest()
                    .post("/rest-post")
                    .route()
                    .setBody(constant("POST: /rest-post"))
                    .endRest();

            from("servlet://hello?matchOnUriPrefix=true")
                    .setBody(constant("GET: /hello"));
        }
    }

}
