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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class BindToRegistryTest {
    @ParameterizedTest
    @ValueSource(strings = { "BindToRegistryOnClassBean", "NestedBindToRegistryBean", "RouteNestedBindToRegistryBean",
            "bindToRegistryByField", "bindToRegistryOnMethod" })
    void bindToRegistryBeanEndpointInvocation(String beanName) {
        RestAssured.given()
                .get("/bean/route/invokeBindToRegistryBean/" + beanName)
                .then()
                .body(equalTo("Hello " + beanName));
    }

    @Test
    void bindToRegistryBeanRetrieveAndInvoke() {
        String beanName = "BindToRegistrySimpleBean";
        RestAssured.given()
                .get("/bean/route/getAndInvokeBindToRegistryBean/" + beanName)
                .then()
                .body(equalTo("Hello " + beanName));
    }
}
