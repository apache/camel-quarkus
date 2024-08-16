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
package org.apache.camel.quarkus.component.microprofile.it.faulttolerance;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.apache.camel.quarkus.component.microprofile.it.faulttolerance.MicroProfileFaultToleranceRoutes.EXCEPTION_MESSAGE;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class MicroprofileFaultToleranceTest {

    @ParameterizedTest
    @MethodSource("routeUris")
    public void testCamelMicroProfileFaultTolerance(String route) {
        // First request should trigger the fallback response
        RestAssured.post("/microprofile-fault-tolerance/route/" + route)
                .then()
                .statusCode(200)
                .body(is(MicroProfileFaultToleranceRoutes.FALLBACK_RESULT));

        // Next request(s) should trigger the expected response
        RestAssured.post("/microprofile-fault-tolerance/route/" + route)
                .then()
                .statusCode(200)
                .body(is(MicroProfileFaultToleranceRoutes.RESULT));
    }

    @ParameterizedTest
    @ValueSource(strings = { "faultToleranceWithThreshold", "circuitBreakerBean" })
    public void testCamelMicroProfileFaultToleranceWithThreshold(String route) {
        // First request should trigger an exception and open the circuit breaker
        RestAssured.post("/microprofile-fault-tolerance/faultToleranceWithThreshold/" + route)
                .then()
                .statusCode(200)
                .body(is(EXCEPTION_MESSAGE));

        // Next request(s) should close the circuit breaker and trigger the expected response
        RestAssured.post("/microprofile-fault-tolerance/faultToleranceWithThreshold/" + route)
                .then()
                .statusCode(200)
                .body(is(MicroProfileFaultToleranceRoutes.RESULT));
    }

    @Test
    public void testCamelMicroProfileFaultToleranceInheritErrorHandler() {
        RestAssured.post("/microprofile-fault-tolerance/inheritErrorHandler")
                .then()
                .statusCode(204);
    }

    public static String[] routeUris() {
        return new String[] {
                "faultToleranceWithFallback",
                "faultToleranceWithBulkhead",
                "faultToleranceWithTimeout",
                "faultToleranceWithTimeoutCustomExecutor",
                "fallbackBean",
                "timeoutBean",
        };
    }
}
