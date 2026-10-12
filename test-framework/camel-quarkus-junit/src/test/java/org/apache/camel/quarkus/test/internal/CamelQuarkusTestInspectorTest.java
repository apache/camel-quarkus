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
package org.apache.camel.quarkus.test.internal;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.quarkus.test.AdviceRoute;
import org.apache.camel.quarkus.test.CamelQuarkusTest;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.apache.camel.quarkus.test.ProvidesBreakpoint;
import org.apache.camel.quarkus.test.ReplaceInRegistry;
import org.apache.camel.spi.Breakpoint;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The fixtures of this test are {@link CamelQuarkusTest} classes that are invalid on purpose. They can only live in
 * this module, whose own test applications do not run the build steps of {@link CamelQuarkusTest}.
 */
class CamelQuarkusTestInspectorTest {

    @Test
    void sharedModeClass() throws IOException {
        CamelQuarkusTestInspector.Result result = inspect(SharedTest.class, SharedTest.Routes.class,
                SharedTest.Advice.class);

        assertEquals(List.of(), result.errors());
        assertEquals(Set.of(SharedTest.class.getName()), result.testClasses());
        assertTrue(result.routesBuilderExcludes().contains(SharedTest.class.getName().replace('.', '/') + "$*"));
        assertTrue(result.routesBuilderExcludes().contains(TopLevelRoutes.class.getName().replace('.', '/')));
        assertEquals(Map.of(), result.isolatedTestClasses());
    }

    @Test
    void isolatedModeClass() throws IOException {
        CamelQuarkusTestInspector.Result result = inspect(IsolatedTest.class);

        assertEquals(List.of(), result.errors());
        assertEquals(Map.of(IsolatedTest.class.getName(), IsolatedTest.class.getName()), result.isolatedTestClasses());
    }

    @Test
    void isolatedOnlyFeaturesWithoutTestProfile() throws IOException {
        CamelQuarkusTestInspector.Result result = inspect(MockWithoutProfileTest.class, BreakpointWithoutProfileTest.class);

        assertErrors(result,
                MockWithoutProfileTest.class.getName()
                        + " uses mockEndpoints, mockEndpointsAndSkip, stubEndpoints, autoStartupExclude, which need the class to own its application, but it has no test profile",
                "@TestProfile(MockWithoutProfileTest.class)",
                BreakpointWithoutProfileTest.class.getName() + " uses @ProvidesBreakpoint");
    }

    @Test
    void isolatedOnlyFeaturesWithSharedTestProfile() throws IOException {
        CamelQuarkusTestInspector.Result result = inspect(SharedProfileStubTest.class, SharedProfileOtherTest.class,
                IsolatedTest.class);

        assertErrors(result, SharedProfileStubTest.class.getName() + " uses stubEndpoints",
                "its test profile " + IsolatedTest.class.getName() + " is also used by "
                        + SharedProfileOtherTest.class.getName());
        assertEquals(Map.of(), result.isolatedTestClasses());
    }

    @Test
    void invalidAttributes() throws IOException {
        CamelQuarkusTestInspector.Result result = inspect(InvalidAttributesTest.class, NoDefaultConstructorRoutes.class,
                AbstractRoutes.class);

        assertErrors(result,
                "replaceRouteFromWith entry 'no-separator' must have the format routeId=uri",
                "replaceRouteFromWith entry 'route=' must have the format routeId=uri",
                "routes class " + NoDefaultConstructorRoutes.class.getName() + " must have a no-arg constructor",
                "routes class " + AbstractRoutes.class.getName() + " must not be abstract",
                "dumpRoute must be \"xml\" or \"yaml\", not \"json\"");
    }

    @Test
    void invalidMembers() throws IOException {
        CamelQuarkusTestInspector.Result result = inspect(InvalidMembersTest.class);

        assertErrors(result,
                "@ProvidesBreakpoint method breakpoint must be public static",
                "@ReplaceInRegistry method bean must take no parameters",
                "@ReplaceInRegistry field staticBean must not be static");
    }

    @Test
    void missingDependencies() throws IOException {
        assertErrors(inspect(DumpTest.class, name -> false),
                "dumpRouteCoverage requires the camel-quarkus-management dependency",
                "dumpRoute = \"yaml\" requires the camel-quarkus-yaml-io dependency");
        assertEquals(List.of(), inspect(DumpTest.class, name -> true).errors());
    }

    @Test
    void unsupportedClasses() throws IOException {
        CamelQuarkusTestInspector.Result result = inspect(ExtendsSupportTest.class, IntegrationTestIT.class,
                OuterTest.class, OuterTest.InnerTest.class);

        assertErrors(result,
                ExtendsSupportTest.class.getName() + " extends CamelQuarkusTestSupport",
                IntegrationTestIT.class.getName() + " is a @QuarkusIntegrationTest",
                OuterTest.InnerTest.class.getName() + " is a @Nested class");
    }

    @Test
    void inheritedAnnotation() throws IOException {
        CamelQuarkusTestInspector.Result result = inspect(AbstractBaseTest.class, ConcreteTest.class);

        assertEquals(List.of(), result.errors());
        assertEquals(Set.of(ConcreteTest.class.getName()), result.testClasses());
        assertEquals(Map.of(ConcreteTest.class.getName(), ConcreteTest.class.getName()), result.isolatedTestClasses());
    }

    @Test
    void invalidBreakpointReturnType() throws IOException {
        assertErrors(inspect(InvalidBreakpointReturnTypeTest.class),
                InvalidBreakpointReturnTypeTest.class.getName()
                        + ": @ProvidesBreakpoint method breakpoint must be public static");
    }

    @Test
    void invalidMembersOfSubclass() throws IOException {
        CamelQuarkusTestInspector.Result result = inspect(AbstractBaseTest.class, InvalidMembersSubclassTest.class);

        assertErrors(result,
                InvalidMembersSubclassTest.class.getName() + ": @ProvidesBreakpoint method breakpoint must be public static",
                InvalidMembersSubclassTest.class.getName() + ": @ReplaceInRegistry field staticBean must not be static");
    }

    @Test
    void routesOfAbstractSubclassAreExcluded() throws IOException {
        CamelQuarkusTestInspector.Result result = inspect(AbstractBaseTest.class, AbstractIntermediateTest.class,
                IntermediateConcreteTest.class);

        assertEquals(List.of(), result.errors());
        assertEquals(Set.of(IntermediateConcreteTest.class.getName()), result.testClasses());
        assertTrue(result.routesBuilderExcludes()
                .contains(AbstractIntermediateTest.class.getName().replace('.', '/') + "$*"));
    }

    private static CamelQuarkusTestInspector.Result inspect(Class<?>... classes) throws IOException {
        return CamelQuarkusTestInspector.inspect(Index.of(classes), name -> true);
    }

    private static CamelQuarkusTestInspector.Result inspect(Class<?> testClass,
            Predicate<String> classPresent) throws IOException {
        return CamelQuarkusTestInspector.inspect(Index.of(testClass), classPresent);
    }

    private static void assertErrors(CamelQuarkusTestInspector.Result result, String... expectedFragments) {
        String errors = String.join("\n", result.errors());
        for (String fragment : expectedFragments) {
            assertTrue(errors.contains(fragment), () -> "Expected an error containing '" + fragment + "' in:\n" + errors);
        }
        IllegalStateException e = assertThrows(IllegalStateException.class,
                result::assertValid);
        assertTrue(e.getMessage().startsWith("Invalid use of @CamelQuarkusTest"));
    }

    @CamelQuarkusTest(routes = { SharedTest.Routes.class,
            TopLevelRoutes.class }, advices = @AdviceRoute(route = "app", advice = SharedTest.Advice.class), replaceRouteFromWith = "app=direct:replaced")
    static class SharedTest {
        static class Routes extends RouteBuilder {
            @Override
            public void configure() {
            }
        }

        static class Advice extends AdviceWithRouteBuilder {
            @Override
            public void configure() {
            }
        }
    }

    @CamelQuarkusTest(mockEndpoints = "*")
    @TestProfile(IsolatedTest.class)
    public static class IsolatedTest implements QuarkusTestProfile {
        @ProvidesBreakpoint
        public static Breakpoint breakpoint() {
            return null;
        }
    }

    @CamelQuarkusTest(mockEndpoints = "*", mockEndpointsAndSkip = "*", stubEndpoints = "kafka", autoStartupExclude = "*")
    static class MockWithoutProfileTest {
    }

    @CamelQuarkusTest
    static class BreakpointWithoutProfileTest {
        @ProvidesBreakpoint
        public static Breakpoint breakpoint() {
            return null;
        }
    }

    @CamelQuarkusTest(stubEndpoints = "kafka")
    @TestProfile(IsolatedTest.class)
    static class SharedProfileStubTest {
    }

    @CamelQuarkusTest
    @TestProfile(IsolatedTest.class)
    static class SharedProfileOtherTest {
    }

    @CamelQuarkusTest(replaceRouteFromWith = { "no-separator", "route=" }, routes = { NoDefaultConstructorRoutes.class,
            AbstractRoutes.class }, dumpRoute = "json")
    static class InvalidAttributesTest {
    }

    @CamelQuarkusTest
    static class InvalidMembersTest {
        @ReplaceInRegistry
        static String staticBean = "bean";

        @ProvidesBreakpoint
        Breakpoint breakpoint() {
            return null;
        }

        @ReplaceInRegistry
        String bean(String name) {
            return name;
        }
    }

    @CamelQuarkusTest(dumpRouteCoverage = true, dumpRoute = "yaml")
    static class DumpTest {
    }

    @CamelQuarkusTest
    static class ExtendsSupportTest extends CamelQuarkusTestSupport {
    }

    @CamelQuarkusTest
    @QuarkusIntegrationTest
    static class IntegrationTestIT {
    }

    @CamelQuarkusTest
    static class OuterTest {
        @Nested
        @CamelQuarkusTest
        class InnerTest {
        }
    }

    @CamelQuarkusTest(autoStartupExclude = "*")
    abstract static class AbstractBaseTest {
    }

    @TestProfile(ConcreteTest.class)
    public static class ConcreteTest extends AbstractBaseTest implements QuarkusTestProfile {
    }

    @CamelQuarkusTest
    @TestProfile(InvalidBreakpointReturnTypeTest.class)
    public static class InvalidBreakpointReturnTypeTest implements QuarkusTestProfile {
        @ProvidesBreakpoint
        public static String breakpoint() {
            return null;
        }
    }

    @TestProfile(InvalidMembersSubclassTest.class)
    public static class InvalidMembersSubclassTest extends AbstractBaseTest implements QuarkusTestProfile {
        @ReplaceInRegistry
        static String staticBean = "bean";

        @ProvidesBreakpoint
        Breakpoint breakpoint() {
            return null;
        }
    }

    abstract static class AbstractIntermediateTest extends AbstractBaseTest {
    }

    @TestProfile(IntermediateConcreteTest.class)
    public static class IntermediateConcreteTest extends AbstractIntermediateTest implements QuarkusTestProfile {
    }

    static class NoDefaultConstructorRoutes extends RouteBuilder {
        NoDefaultConstructorRoutes(String name) {
        }

        @Override
        public void configure() {
        }
    }

    abstract static class AbstractRoutes extends RouteBuilder {
    }
}

class TopLevelRoutes extends RouteBuilder {
    @Override
    public void configure() {
    }
}
