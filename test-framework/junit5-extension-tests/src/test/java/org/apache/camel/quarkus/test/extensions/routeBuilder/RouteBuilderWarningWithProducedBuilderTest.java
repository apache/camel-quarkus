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
package org.apache.camel.quarkus.test.extensions.routeBuilder;

import java.util.concurrent.TimeUnit;

import io.quarkus.test.ContinuousTestingTestUtils;
import io.quarkus.test.QuarkusDevModeTest;
import org.apache.camel.quarkus.test.extensions.continousDev.HelloResource;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Scenario when useRouteBuilder is TRUE and RouteBuilder is created via HelloRouteBuilder -> should succeed with
 * warning.
 */
public class RouteBuilderWarningWithProducedBuilderTest {

    @RegisterExtension
    static final QuarkusDevModeTest TEST = RouteBuilderUtil.createTestModule(RouteBuilderWarningET.class,
            RouteBuilderWarningResource.class, HelloResource.class, RouteBuilderHello.class);

    @Test
    public void checkTests() {
        ContinuousTestingTestUtils utils = new ContinuousTestingTestUtils();
        ContinuousTestingTestUtils.TestStatus ts = utils.waitForNextCompletion();

        Assertions.assertEquals(0L, ts.getTestsFailed());
        Assertions.assertEquals(1L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> TEST.getLogRecords().stream()
                .anyMatch(logRecord -> logRecord.getMessage().contains("`RouteBuilder` detected")));
    }
}
