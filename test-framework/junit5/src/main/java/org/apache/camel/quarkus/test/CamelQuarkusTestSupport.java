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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.callback.QuarkusTestContext;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.NoSuchEndpointException;
import org.apache.camel.Processor;
import org.apache.camel.Route;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.quarkus.core.FastCamelContext;
import org.apache.camel.spi.Registry;
import org.apache.camel.test.junit5.AbstractTestSupport;
import org.apache.camel.test.junit5.CamelContextManager;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.apache.camel.test.junit5.ContextManagerFactory;
import org.apache.camel.test.junit5.TestSupport;
import org.apache.camel.test.junit5.util.ExtensionHelper;
import org.apache.camel.test.junit5.util.RouteCoverageDumperExtension;
import org.apache.camel.util.StopWatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link CamelTestSupport} class does not work on Quarkus. This class provides a replacement, which can be used in
 * JVM mode. Note that {@link CamelQuarkusTestSupport} <b>DOES NOT</b> work for native mode tests.
 * <p>
 * There are several differences between {@link CamelTestSupport} and this class.
 * </p>
 * <ul>
 * <li>Starting and stopping {@link CamelContext} in Camel Quarkus is generally bound to starting and stopping the
 * application
 * and this holds also when testing.</li>
 * <li>Starting and stopping the application under test (and thus also {@link CamelContext} is under full control of
 * Quarkus
 * JUnit Extension. It prefers keeping the application up and running unless it is told to do otherwise.</li>
 * <li>Hence normally the application under test is started only once for all test classes of the given Maven/Gradle
 * module.</li>
 * <li>To force Quarkus JUnit Extension to restart the application (and thus also `CamelContext`) for a given test
 * class, you need to assign a unique {@code @io.quarkus.test.junit.TestProfile} to that class. Check the
 * <a href="https://quarkus.io/guides/getting-started-testing#testing_different_profiles">Quarkus documentation</a> for
 * how you can do
 * that. (Note that
 * <a href="https://quarkus.io/guides/getting-started-testing#quarkus-test-resource">QuarkusTestResource</a>
 * has a similar effect.)</li>
 * <li>Camel Quarkus executes the production of beans during the build phase. Because all the tests are
 * build together, exclusion behavior is implemented into {@link CamelQuarkusTestSupport}. If a producer of the specific
 * type
 * and name is used in one tests, the instance will be the same for the rest of the tests.</li>
 * <li>JUnit Jupiter callbacks {@code BeforeEachCallback}, {@code AfterEachCallback}, {@code AfterAllCallback},
 * {@code BeforeAllCallback},
 * {@code BeforeTestExecutionCallback} and {@code AfterTestExecutionCallback}) might not work correctly. See the
 * <a href="https://quarkus.io/guides/getting-started-testing#enrichment-via-quarkustestcallback">documentation</a>.
 * Methods {@code afterAll}, {@code afterEach}, {@code afterTestExecution}, {@code beforeAll} and {@code beforeEach} are
 * not executed anymore.
 * You should use {@code doAfterAll}, {@code doAfterConstruct}, {@code doAfterEach}, {@code doBeforeEach} and
 * {@code doBeforeAll} instead of
 * them.</li>
 * </ul>
 */
public class CamelQuarkusTestSupport extends AbstractTestSupport
        implements QuarkusTestProfile {

    private static final Logger LOG = LoggerFactory.getLogger(CamelQuarkusTestSupport.class);

    //    @RegisterExtension
    //    protected CamelTestSupport camelTestSupportExtension = this;

    private final StopWatch watch = new StopWatch();
    private String currentTestName;

    private CamelContextManager contextManager;
    private final ContextManagerFactory contextManagerFactory;

    @Inject
    protected CamelContext context;
    private Set<String> createdRoutes;

    public CamelQuarkusTestSupport() {
        super(new CustomTestExecutionConfiguration(), new CustomCamelContextConfiguration());

        this.contextManagerFactory = new ContextNotStoppingManagerFactory();

        testConfigurationBuilder()
                .withCustomUseAdviceWith(isUseAdviceWith())
                .withJMX(useJmx())
                .withUseRouteBuilder(isUseRouteBuilder())
                .withDumpRouteCoverage(isDumpRouteCoverage())
                .withAutoStartContext(false);

        contextConfiguration()
                .withCustomCamelContextSupplier(this::camelContextSupplier)
                .withCustomPostProcessor(this::postProcessTest)
                .withCustomRoutesSupplier(this::createRouteBuilders)
                .withRegistryBinder(this::bindToRegistry)
                .withUseOverridePropertiesWithPropertiesComponent(useOverridePropertiesWithPropertiesComponent())
                .withRouteFilterExcludePattern(getRouteFilterExcludePattern())
                .withRouteFilterIncludePattern(getRouteFilterIncludePattern())
                .withMockEndpoints(isMockEndpoints())
                .withMockEndpointsAndSkip(isMockEndpointsAndSkip());

        //CQ starts and stops context with the application start/stop
        testConfiguration().withAutoStartContext(false);
    }

    CustomCamelContextConfiguration contextConfiguration() {
        return (CustomCamelContextConfiguration) camelContextConfiguration;
    }

    CustomTestExecutionConfiguration testConfigurationBuilder() {
        return (CustomTestExecutionConfiguration) testConfigurationBuilder;
    }

    //------------------------ quarkus callbacks ---------------

    /**
     * Replacement of {@link #afterAll(ExtensionContext)} called from {@link AfterAllCallback#afterAll(QuarkusTestContext)}
     */
    protected void doAfterAll(QuarkusTestContext context) throws Exception {
        // Noop
    }

    /**
     * Replacement of {@link #afterEach(ExtensionContext)} called from
     * {@link AfterEachCallback#afterEach(QuarkusTestMethodContext)}
     */
    protected void doAfterEach(QuarkusTestMethodContext context) throws Exception {
        // Noop
    }

    /**
     * Replacement of {@link #beforeAll(ExtensionContext)} called from {@link AfterConstructCallback#afterConstruct(Object)}
     * Execution differs in case of <i>@TestInstance(TestInstance.Lifecycle.PER_METHOD)</i>. in which case a callback is
     * invoked
     * before each test (instead of {@link #beforeAll(ExtensionContext)}).
     */
    protected void doAfterConstruct() throws Exception {
        // Noop
    }

    /**
     * Replacement of {@link #beforeEach(ExtensionContext)} called from
     * {@link BeforeEachCallback#beforeEach(QuarkusTestMethodContext)}
     */
    protected void doBeforeEach(QuarkusTestMethodContext context) throws Exception {
        // Noop
    }

    /**
     * Feel free to override this method for the sake of customizing the instance returned by this implementation.
     * Do not create your own CamelContext instance, because there needs to exist just a single instance owned by
     * Quarkus CDI container. There are checks in place that will make your tests fail if you do otherwise.
     *
     * @return           The context from Quarkus CDI container
     * @throws Exception Overridden method has to throw the same Exception as superclass.
     */
    protected CamelContext camelContextSupplier() throws Exception {
        return this.context;
    }

    /**
     * The same functionality as {@link CamelTestSupport#bindToRegistry(Registry)}.
     */
    protected void bindToRegistry(Registry registry) throws Exception {
        assertTestClassCamelContextMatchesAppCamelContext();
    }

    /**
     * The same functionality as {@link CamelTestSupport#postProcessTest()} .
     */
    protected void postProcessTest() throws Exception {
        assertTestClassCamelContextMatchesAppCamelContext();

        template = contextManager.template();
        fluentTemplate = contextManager.fluentTemplate();
        consumer = contextManager.consumer();
    }

    /**
     * The same functionality as {@link CamelTestSupport#context()} .
     */
    @Override
    public CamelContext context() {
        return this.context;
    }

    @Deprecated(since = "4.7.0")
    public long timeTaken() {
        return watch.taken();
    }

    /**
     * Gets the name of the current test being executed.
     */
    public final String getCurrentTestName() {
        return currentTestName;
    }

    /**
     * Common test setup. For internal use.
     *
     * @deprecated           Use {@link #setupResources()} instead
     * @throws     Exception if unable to setup the test
     */
    @Deprecated(since = "4.7.0")
    public void setUp() throws Exception {
        ExtensionHelper.testStartHeader(getClass(), currentTestName);

        setupResources();
        doPreSetup();

        contextManager.createCamelContext(this);
        context = contextManager.context();

        // Log a warning in case that at least one RouteBuilder is present in the Camel registry.
        // It might mean, that unintentionally routes are shared across tests, or that a RouteBuilder is
        // created with @Produces
        if (isUseRouteBuilder() && !context.getRegistry().findByType(RouteBuilder.class).isEmpty()) {
            LOG.warn("isUseRouteBuilder = true and RouteBuilder beans are present in the Camel registry.\n" +
                    "All tests will share their routes. If this is not desired, define your test routes " +
                    "by overriding CamelQuarkusTestSupport.createRouteBuilder().\nOr use configuration properties " +
                    "quarkus.camel.routes-discovery.exclude-patterns or quarkus.camel.routes-discovery.include-patterns " +
                    "to control which routes are started.");
        }

        doPostSetup();

        // only start timing after all the setup
        watch.restart();
    }

    /**
     * Common test tear down. For internal use.
     *
     * @deprecated           Use {@link #cleanupResources()} instead
     * @throws     Exception if unable to setup the test
     */
    @Deprecated(since = "4.7.0")
    @AfterEach
    public void tearDown() throws Exception {
        long time = watch.taken();

        if (isRouteCoverageEnabled()) {
            ExtensionHelper.testEndFooter(getClass(), currentTestName, time,
                    new RouteCoverageDumperExtension((ModelCamelContext) context));
        } else {
            ExtensionHelper.testEndFooter(getClass(), currentTestName, time);
        }

        if (testConfigurationBuilder.isCreateCamelContextPerClass()) {
            // will tear down test specially in afterAll callback
            return;
        }

        LOG.debug("tearDown()");

        contextManager.stop();

        doPostTearDown();
        cleanupResources();

    }

    /**
     * Strategy to perform any post-action, after {@link CamelContext} is stopped. This is meant for internal Camel
     * usage and should not be used by user classes.
     *
     * @deprecated use {@link #cleanupResources()} instead.
     */
    @Deprecated(since = "4.7.0")
    protected void doPostTearDown() throws Exception {
        // noop
    }

    /**
     * Factory method which derived classes can use to create a {@link RouteBuilder} to define the routes for testing
     */
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() {
                // no routes added by default
            }
        };
    }

    /**
     * Factory method which derived classes can use to create an array of {@link org.apache.camel.builder.RouteBuilder}s
     * to define the routes for testing
     *
     * @see        #createRouteBuilder()
     * @deprecated This method will be made private. Do not use
     */
    @Deprecated(since = "4.7.0")
    protected RoutesBuilder[] createRouteBuilders() throws Exception {
        return new RoutesBuilder[] { createRouteBuilder() };
    }

    void internalAfterAll(QuarkusTestContext context, ExtensionContext extensionContext) {
        try {
            if (testConfiguration().isCreateCamelContextPerClass()) {
                //call all clear and release methods, stop is not called as it is disabled on the camelContextManagers
                contextManager.stop();
            } else {
                doPostTearDown();
            }
            cleanupResources();

        } catch (Exception e) {
            // ignore
        }
    }

    void internalBeforeAll(ExtensionContext context) {
        final boolean perClassPresent = context.getTestInstanceLifecycle()
                .filter(lc -> lc.equals(TestInstance.Lifecycle.PER_CLASS)).isPresent();
        if (perClassPresent) {
            LOG.trace("Creating a legacy context manager for {}", context.getDisplayName());
            testConfigurationBuilder().withCustomCreateCamelContextPerClass(perClassPresent);
            contextManager = contextManagerFactory.createContextManager(ContextManagerFactory.Type.BEFORE_ALL,
                    testConfigurationBuilder, camelContextConfiguration);
        }

        ExtensionContext.Store globalStore = context.getStore(ExtensionContext.Namespace.GLOBAL);
        contextManager.setGlobalStore(globalStore);
    }

    void internalBeforeEach(ExtensionContext context) throws Exception {
        if (contextManager == null) {
            LOG.trace("Creating a transient context manager for {}", context.getDisplayName());
            contextManager = contextManagerFactory.createContextManager(ContextManagerFactory.Type.BEFORE_EACH,
                    testConfigurationBuilder, camelContextConfiguration);
        }

        currentTestName = context.getDisplayName();
        ExtensionContext.Store globalStore = context.getStore(ExtensionContext.Namespace.GLOBAL);
        contextManager.setGlobalStore(globalStore);
    }

    /**
     * Strategy to perform any pre setup, before the {@link CamelContext} is created.
     * <p>
     * Be aware that difference in lifecycle with Quarkus may require special behavior.
     * If this method is overridden, <i>super.doPreSetup()</i> must be called.
     * </p>
     */
    protected void doPreSetup() throws Exception {
        if (isUseAdviceWith() || isUseDebugger()) {
            ((FastCamelContext) context).suspend();
        }

        if (isUseRouteBuilder()) {
            // Save the routeIds of routes existing before setup
            createdRoutes = context.getRoutes()
                    .stream()
                    .map(Route::getRouteId)
                    .collect(Collectors.toSet());
        }
    }

    /**
     * Strategy to perform any post setup after the {@link CamelContext} is created.
     * <p>
     * Be aware that difference in lifecycle with Quarkus may require special behavior.
     * If this method is overridden, <i>super.doPostSetup()</i> must be called.
     * </p>
     */
    protected void doPostSetup() throws Exception {
        if (isUseAdviceWith() || isUseDebugger()) {
            ((FastCamelContext) context).resume();
            if (isUseDebugger()) {
                ModelCamelContext mcc = (ModelCamelContext) context;
                List<RouteDefinition> rdfs = mcc.getRouteDefinitions();
                // If the context was suspended, routes were not added because it would trigger start of the context
                // therefore, add the routes here
                mcc.addRouteDefinitions(rdfs);
            }
        }

        if (isUseRouteBuilder()) {
            // Remove from the routes all routes which existed before setup
            Set<String> allRoutes = context.getRoutes()
                    .stream()
                    .map(Route::getRouteId)
                    .collect(Collectors.toSet());
            if (createdRoutes != null) {
                allRoutes.removeAll(createdRoutes);
            }
            createdRoutes = allRoutes;
        }
    }

    /**
     * Override when using <a href="http://camel.apache.org/advicewith.html">advice with</a> and return <code>true</code>.
     * <p/>
     * <b>Important:</b> You must execute method {@link #startRouteDefinitions()}} manually from the unit test
     * after you are done doing all the advice with.
     *
     * @return <code>true</code> to apply advice to existing route(s). <code>false</code> to disable advice.
     */
    @Override
    public boolean isUseAdviceWith() {
        return false;
    }

    /**
     * Helper method to start routeDefinitions (to be used with `adviceWith`).
     */
    protected void startRouteDefinitions() throws Exception {
        ModelCamelContext modelCamelContext = (ModelCamelContext) context;
        List<RouteDefinition> definitions = new ArrayList<>(modelCamelContext.getRouteDefinitions());
        for (Route r : context.getRoutes()) {
            //existing route does not need to be started
            definitions.remove(r.getRoute());
        }
        modelCamelContext.startRouteDefinitions(definitions);
    }

    /**
     * Resolves the mandatory Mock endpoint using a URI of the form <code>mock:someName</code>
     *
     * @param  uri the URI which typically starts with "mock:" and has some name
     * @return     the mandatory mock endpoint or an exception is thrown if it could not be resolved
     */
    protected final MockEndpoint getMockEndpoint(String uri) {
        return getMockEndpoint(uri, true);
    }

    /**
     * Resolves the {@link MockEndpoint} using a URI of the form <code>mock:someName</code>, optionally creating it if
     * it does not exist. This implementation will lookup existing mock endpoints and match on the mock queue name, eg
     * mock:foo and mock:foo?retainFirst=5 would match as the queue name is foo.
     *
     * @param  uri                     the URI which typically starts with "mock:" and has some name
     * @param  create                  whether to allow the endpoint to be created if it doesn't exist
     * @return                         the mock endpoint or an {@link NoSuchEndpointException} is thrown if it could not
     *                                 be resolved
     * @throws NoSuchEndpointException is the mock endpoint does not exist
     */
    @Deprecated(since = "4.7.0")
    protected final MockEndpoint getMockEndpoint(String uri, boolean create) throws NoSuchEndpointException {
        return TestSupport.getMockEndpoint(context, uri, create);
    }

    /**
     * Single step debugs and Camel invokes this method before entering the given processor. This method is NOOP.
     *
     * @deprecated Use {@link #camelContextConfiguration()} to set an instance of {@link DebugBreakpoint}
     */
    @Deprecated(since = "4.7.0")
    protected void debugBefore(
            Exchange exchange, Processor processor, ProcessorDefinition<?> definition, String id, String label) {
    }

    /**
     * Single step debugs and Camel invokes this method after processing the given processor. This method is NOOP.
     *
     * @deprecated Use {@link #camelContextConfiguration()} to set an instance of {@link DebugBreakpoint}
     */
    @Deprecated(since = "4.7.0")
    protected void debugAfter(
            Exchange exchange, Processor processor, ProcessorDefinition<?> definition, String id, String label,
            long timeTaken) {
    }

    Set<String> getCreatedRoutes() {
        return createdRoutes;
    }

    private void assertTestClassCamelContextMatchesAppCamelContext() {
        // Test classes must use the same CamelContext as the application under test
        Assertions.assertEquals(context, super.context,
                "CamelQuarkusTestSupport uses a different CamelContext compared to the application under test");
    }
}
