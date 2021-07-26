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
package org.apache.camel.quarkus.support.spring.test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class SpringSupportTest {

    //@Test
    public void springClassLoading() {
        // Verify that classes excluded by the Quarkus Spring extensions can be loaded
        // I.e check they are not blacklisted since the camel-quarkus-support-spring-(beans,context,core) jars will provide them
        String[] classNames = new String[] {
                // From: org.springframework:spring-beans
                "org.springframework.beans.factory.InitializingBean",
                // From: org.springframework:spring-context
                "org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor",
                // From: org.springframework:spring-core
                "org.springframework.core.SpringVersion",
        };

        for (String className : classNames) {
            RestAssured.given()
                    .pathParam("className", className)
                    .when()
                    .get("/classloading/{className}")
                    .then()
                    .statusCode(204);
        }
    }
}
