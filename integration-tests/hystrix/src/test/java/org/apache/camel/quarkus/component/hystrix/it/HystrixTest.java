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
package org.apache.camel.quarkus.component.hystrix.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import static org.hamcrest.core.Is.is;

@QuarkusTest
@QuarkusTestResource(HystrixTestResource.class)
// https://github.com/apache/camel-quarkus/issues/1146
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class HystrixTest {

    //@Test
    public void testHystrixFallback() {

        // Try a 10 millisecond delay in route processing to be within the allowed circuit breaker tolerance
        RestAssured.get("/hystrix/fallback/delay/10")
                .then()
                .statusCode(200)
                .body(is("Hello Camel Quarkus Hystrix"));

        // Try an unacceptable delay to trigger the fallback response
        RestAssured.get("/hystrix/fallback/delay/110")
                .then()
                .statusCode(200)
                .body(is("Fallback response"));
    }

    //@Test
    public void testHystrixFallbackViaNetwork() {

        RestAssured.get("/hystrix/fallback/network")
                .then()
                .statusCode(200)
                .body(is("Fallback via network response"));
    }
}
