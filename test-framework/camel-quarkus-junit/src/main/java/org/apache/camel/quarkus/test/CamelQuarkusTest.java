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
package org.apache.camel.quarkus.test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.test.junit.QuarkusTest;
import org.apache.camel.RoutesBuilder;

/**
 * Marks a JVM mode Quarkus test that uses Camel specific test features. Implies {@link QuarkusTest}.
 * <p>
 * Everything declared on this annotation applies to the whole test class: it is set up before the first test method
 * runs and undone after the last one.
 * <p>
 * A test class runs in <em>isolated mode</em> when it is annotated with
 * {@code @io.quarkus.test.junit.TestProfile} and no other {@code @CamelQuarkusTest} class uses the same profile. The
 * class then owns the application it runs in and the plan is applied before any route is created. Otherwise the
 * class runs in <em>shared mode</em>, where {@link #mockEndpoints()}, {@link #mockEndpointsAndSkip()},
 * {@link #stubEndpoints()}, {@link #autoStartupExclude()} and {@link ProvidesBreakpoint} cannot be used.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@QuarkusTest
public @interface CamelQuarkusTest {

    /**
     * {@link RoutesBuilder} classes that exist only for this test class. Their routes are added before the first test
     * method and removed after the last one. The classes must have an accessible no-arg constructor. They are never
     * discovered as application routes.
     */
    Class<? extends RoutesBuilder>[] routes() default {};

    /**
     * Advice to apply to routes before the first test method.
     */
    AdviceRoute[] advices() default {};

    /**
     * Route consumer endpoints to replace before the first test method, in the format {@code routeId=uri}.
     */
    String[] replaceRouteFromWith() default {};

    /**
     * Pattern of endpoints to mock. Messages are sent to the mock endpoint and then to the original endpoint. Isolated
     * mode only.
     */
    String mockEndpoints() default "";

    /**
     * Pattern of endpoints to mock. Messages are sent to the mock endpoint only. Isolated mode only.
     */
    String mockEndpointsAndSkip() default "";

    /**
     * Comma separated names of components whose endpoints are replaced with stub endpoints, for example
     * {@code kafka,amqp}. Isolated mode only.
     */
    String stubEndpoints() default "";

    /**
     * Pattern of route ids that are not started automatically. Isolated mode only.
     */
    String autoStartupExclude() default "";

    /**
     * Whether to dump route coverage to {@code target/camel-route-coverage} after each test method. Requires
     * {@code camel-quarkus-management}.
     */
    boolean dumpRouteCoverage() default false;

    /**
     * Format to dump routes to {@code target/camel-route-dump} after each test method: {@code xml} or {@code yaml}.
     * {@code xml} requires {@code camel-quarkus-xml-io-dsl}, {@code yaml} requires {@code camel-quarkus-yaml-io}.
     */
    String dumpRoute() default "";

    /**
     * Timeout in seconds for graceful shutdown of routes.
     */
    int shutdownTimeout() default 10;
}
