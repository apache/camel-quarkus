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
package org.apache.camel.quarkus.test.annotation;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs in the same application as the {@code @CamelQuarkusTest} classes without a test profile.
 */
@QuarkusTest
class RoutesNotGlobalTest {

    @Inject
    CamelContext context;

    @Test
    void testRoutesAreNotApplicationRoutes() {
        assertThat(context.getRoute(RoutesTest.ROUTE_ID)).isNull();
        assertThat(context.getRoute(RoutesOtherTest.ROUTE_ID)).isNull();
        assertThat(context.getRoute(RoutesOtherTest.UNLISTED_ROUTE_ID)).isNull();
        assertThat(context.getRoute(AnnotationApplicationRoutes.ROUTE_ID)).isNotNull();
    }
}
