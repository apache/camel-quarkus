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
package org.apache.camel.quarkus.core.faulttolerance.it;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class CoreFaultToleranceTest {

    @Test
    public void testFaultTolerancePropertiesAreApplied() {
        get("/core/fault-tolerance-configurations")
                .then()
                .body(
                        "isCustomCircuitBreakerRef", is(true),
                        "delay", is(15),
                        "successThreshold", is(4),
                        "requestVolumeThreshold", is(60),
                        "failureRatio", is(94),
                        "timeoutEnabled", is(true),
                        "timeoutDuration", is(3000),
                        "timeoutPoolSize", is(3),
                        "bulkheadEnabled", is(false),
                        "bulkheadMaxConcurrentCalls", is(20),
                        "bulkheadWaitingTaskQueue", is(21),
                        "isCustomBulkheadExecutorServiceRef", is(true));
    }

}
