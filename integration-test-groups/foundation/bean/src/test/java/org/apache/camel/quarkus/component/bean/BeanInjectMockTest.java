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
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
public class BeanInjectMockTest {

    @InjectMock
    NamedBean mockNamedBean;

    @BeforeEach
    public void setup() {
        when(mockNamedBean.hello(anyString())).thenReturn("Hello * from NamedBean mock (class level)");
    }

    //@Test
    public void namedBeanMockedForAllTests() {
        RestAssured.given()
                .body("Kermit")
                .post("/bean/route/named")
                .then()
                .body(is("Hello * from NamedBean mock (class level)"));
    }

    //@Test
    public void namedBeanMockOverrriddenInATest() {
        when(mockNamedBean.hello(anyString())).thenReturn("Hello * from NamedBean mock (test level)");
        RestAssured.given()
                .body("Kermit")
                .post("/bean/route/named")
                .then()
                .body(is("Hello * from NamedBean mock (test level)"));
    }

}
