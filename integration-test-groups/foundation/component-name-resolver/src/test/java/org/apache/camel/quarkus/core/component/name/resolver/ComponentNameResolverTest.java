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
package org.apache.camel.quarkus.core.component.name.resolver;

import java.util.List;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.quarkus.core.FastComponentNameResolver;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class ComponentNameResolverTest {

    @Test
    public void fastComponentNameResolverConfigured() {
        RestAssured.get("/component-name-resolver/class")
                .then()
                .statusCode(200)
                .body(is(FastComponentNameResolver.class.getName()));
    }

    @Test
    public void resolveComponentNames() {
        String rawComponentNames = RestAssured.get("/component-name-resolver/resolve")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        List<String> componentNames = List.of(rawComponentNames.split(","));
        assertTrue(componentNames.contains("direct"));
        assertTrue(componentNames.contains("log"));
        assertTrue(componentNames.contains("mock"));
        assertFalse(componentNames.contains("cron"));
    }
}
