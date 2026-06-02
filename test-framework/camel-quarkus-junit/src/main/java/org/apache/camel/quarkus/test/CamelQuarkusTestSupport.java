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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.quarkus.core.FastCamelContext;
import org.apache.camel.quarkus.test.CallbackUtil.MockExtensionContext;
import org.apache.camel.spi.Registry;
import org.apache.camel.test.junit6.AbstractTestSupport;
import org.apache.camel.test.junit6.CamelContextConfiguration;
import org.apache.camel.test.junit6.CamelContextManager;
import org.apache.camel.test.junit6.CamelTestSupport;
import org.apache.camel.test.junit6.ContextManagerFactory;
import org.apache.camel.test.junit6.TestExecutionConfiguration;
import org.apache.camel.test.junit6.TestSupport;
import org.apache.camel.test.junit6.util.ExtensionHelper;
import org.apache.camel.test.junit6.util.RouteCoverageDumperExtension;
import org.apache.camel.util.StopWatch;
import org.apache.camel.util.function.ThrowingConsumer;
import org.eclipse.microprofile.config.ConfigProvider;
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

    private final StopWatch watch = new StopWatch();
    private String currentTestName;

    private CamelContextManager contextManager;
    private final ContextManagerFactory contextManagerFactory;

    @Inject
    protected CamelContext context;
    private Set<String> createdRoutes;
    private Map<String, List<ProcessorDefinition<?>>> originalRouteOutputs;
    private Set<String> existingComponents;

    public CamelQuarkusTestSupport() {
        super(new CamelQuarkusTestExecutionConfiguration(), new CamelQuarkusContextConfiguration());

        this.contextManagerFactory = new ContextNotStoppingManagerFactory();

        testConfigurationBuilder()
                .withUseAdviceWith(isUseAdviceWith())
                .withJMX(useJmx())
                .withUseRouteBuilder(isUseRouteBuilder())
                .withDumpRouteCoverage(isDumpRouteCoverage())
                .withAutoStartContext(false);

        contextConfiguration()
                .withCamelContextSupplier(this::camelContextSupplier)
                .withPostProcessor(this::postProcessTest)
                .withRoutesSupplier(this::createRouteBuilders)
                .withRegistryBinder(this::bindToRegistry)
                .withUseOverridePropertiesWithPropertiesComponent(useOverridePropertiesWithPropertiesComponent())
                .withRouteFilterExcludePattern(getRouteFilterExcludePattern())
                .withRouteFilterIncludePattern(getRouteFilterIncludePattern())
                .withMockEndpoints(isMockEndpoints())
                .withMockEndpointsAndSkip(isMockEndpointsAndSkip())
                .withShutdownTimeout(ConfigProvider.getConfig()
                        .getOptionalValue("camel.main.shutdownTimeout", Integer.class)
                        .orElse(10));

        //CQ starts and stops context with the application start/stop
        testConfiguration().withAutoStartContext(false);
    }

    CamelQuarkusContextConfiguration contextConfiguration() {
        return (CamelQuarkusContextConfiguration) camelContextConfiguration;
    }

    CamelQuarkusTestExecutionConfiguration testConfigurationBuilder() {
        return (CamelQuarkusTestExecutionConfiguration) testConfigurationBuilder;
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
    @Deprecated(since = "3.15.0")
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

    @Deprecated(since = "3.15.0")
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
    @Deprecated(since = "3.15.0")
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
    @Deprecated(since = "3.15.0")
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
    @Deprecated(since = "3.15.0")
    protected void doPostTearDown() throws Exception {
        // noop
    }

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
    @Deprecated(since = "3.15.0")
    protected RoutesBuilder[] createRouteBuilders() throws Exception {
        return new RoutesBuilder[] { createRouteBuilder() };
    }

    void internalAfterAll() {
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

    void internalBeforeAll(MockExtensionContext context) {
        final boolean perClassPresent = context.getTestInstanceLifecycle()
                .filter(lc -> lc.equals(TestInstance.Lifecycle.PER_CLASS)).isPresent();
        if (perClassPresent) {
            LOG.trace("Creating a legacy context manager for {}", context.getDisplayName());
            testConfigurationBuilder().withCreateCamelContextPerClass(perClassPresent);
            contextManager = contextManagerFactory.createContextManager(ContextManagerFactory.Type.BEFORE_ALL,
                    testConfigurationBuilder, camelContextConfiguration);
        }

        ExtensionContext.Store globalStore = context.getStore(ExtensionContext.Namespace.GLOBAL);
        contextManager.setGlobalStore(globalStore);
    }

    void internalBeforeEach(MockExtensionContext context) throws Exception {
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
    @Deprecated(since = "3.15.0")
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

        // Save a snapshot of route outputs before any advice is applied
        // This allows us to restore routes to their original state between tests
        ModelCamelContext modelContext = context.getCamelContextExtension().getContextPlugin(ModelCamelContext.class);
        if (modelContext != null) {
            originalRouteOutputs = new HashMap<>();
            for (RouteDefinition routeDef : modelContext.getRouteDefinitions()) {
                // Save a copy of the outputs list (the list itself, not cloning each processor)
                List<ProcessorDefinition<?>> outputs = new ArrayList<>(routeDef.getOutputs());
                originalRouteOutputs.put(routeDef.getId(), outputs);
            }
        }

        // Save a snapshot of existing components before the test creates any new ones
        // This allows us to only remove test-created components during cleanup
        existingComponents = new HashSet<>(context.getComponentNames());
    }

    /**
     * Strategy to perform any post setup after the {@link CamelContext} is created.
     * <p>
     * Be aware that difference in lifecycle with Quarkus may require special behavior.
     * If this method is overridden, <i>super.doPostSetup()</i> must be called.
     * </p>
     */
    @Deprecated(since = "3.15.0")
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
     * Override when using <a href="https://camel.apache.org/manual/advice-with.html">advice with</a> and return
     * <code>true</code>.
     * <p/>
     * <b>Advice Cleanup:</b> AdviceWith modifications are automatically cleaned up between test methods. This ensures
     * that advice applied in one test does not persist to subsequent tests, preventing interference between tests.
     * <p/>
     * <b>Fundamental Limitation:</b> In Camel Quarkus, the CamelContext is assembled at build time and shared across
     * all tests in a module. Unlike standalone Camel or Spring Boot (where the context can be manually started in each
     * test), you cannot replay the build steps to get a fresh context. This means:
     * <ul>
     * <li>Routes may have already auto-started before advice can be applied</li>
     * <li>Routes that connect to external systems (Kafka, JMS, etc.) may briefly attempt to connect before being
     * suspended</li>
     * <li>The context cannot be fully recreated between tests without using different {@code @TestProfile}s</li>
     * </ul>
     * <p/>
     * <b>Workaround for Auto-Starting Routes:</b> If routes must not start (e.g., to avoid connection attempts), use
     * {@code autoStartup=false} in your route definition or via properties (e.g.,
     * {@code quarkus.camel.routes.auto-startup=false}). When using this deprecated approach, you must manually call
     * {@link #startRouteDefinitions()} after applying advice.
     * <p/>
     * <b>Recommended Approach:</b> Instead of using this flag, apply advice in
     * {@link #doBeforeEach(QuarkusTestMethodContext)}.
     * This approach is simpler and works reliably for both routes defined in the test class and global routes
     * (YAML routes or RouteBuilder beans). Routes that aren't advised will be automatically started after
     * {@code doBeforeEach()} completes. Note that the fundamental build-time limitation still applies.
     *
     * @return     <code>true</code> to suspend context during setup for applying advice. <code>false</code> to disable.
     * @deprecated While this method works, it has fundamental lifecycle limitations in Camel Quarkus due to the
     *             build-time context assembly. The recommended approach is to apply advice in
     *             {@link #doBeforeEach(QuarkusTestMethodContext)}. Advice cleanup between tests and automatic route
     *             starting happen automatically regardless of which approach you use.
     */
    @Override
    @Deprecated(since = "3.15.0")
    public boolean isUseAdviceWith() {
        return false;
    }

    /**
     * Helper method to start routeDefinitions (to be used with `adviceWith`).
     * <p>
     * <b>Note:</b> This method is only necessary when using the deprecated {@link #isUseAdviceWith()} approach.
     * If you apply AdviceWith in {@link #doBeforeEach(QuarkusTestMethodContext)}, route definitions are
     * automatically started after {@code doBeforeEach()} completes, so you don't need to call this method.
     * <p>
     * When using {@code isUseAdviceWith() = true}, you must still call this method manually after applying
     * advice in your {@code @BeforeEach} methods to start routes that weren't advised.
     *
     * @throws Exception if unable to start route definitions
     */
    protected void startRouteDefinitions() throws Exception {
        ModelCamelContext modelCamelContext = context.getCamelContextExtension().getContextPlugin(ModelCamelContext.class);
        List<RouteDefinition> definitions = new ArrayList<>(modelCamelContext.getRouteDefinitions());
        for (Route r : context.getRoutes()) {
            //existing route does not need to be started
            definitions.remove(r.getRoute());
        }
        modelCamelContext.startRouteDefinitions(definitions);
    }

    /**
     * Helper method to apply AdviceWith to a route.
     * <p>
     * This method should be used to advise routes that already exist in your application
     * code (src/main/java), and NOT routes created via {@link #createRouteBuilder()} in your test class.
     * Advising test-created routes can cause unpredictable behavior.
     * <p>
     * This method handles the route lifecycle automatically:
     * <ol>
     * <li>Stops the route</li>
     * <li>Applies the advice</li>
     * <li>Starts the route</li>
     * </ol>
     * <p>
     * You can use this when you need <b>different</b> advice per test method on existing routes
     * <p>
     * <b>Example - advising existing application route:</b>
     *
     * <pre>
     * &#64;code
     * &#64;QuarkusTest
     * public class MyRouteTest extends CamelQuarkusTestSupport {
     *     @Test
     *     public void testWithMock() throws Exception {
     *         // Advise an existing route from your application
     *         adviceRoute("existing-route-id", route -> {
     *             route.weaveByToUri("kafka:real-topic").replace().to("mock:result");
     *         });
     *         // ... test assertions
     *     }
     * }
     * </pre>
     * <p>
     *
     * @param  routeId   the ID of the <b>EXISTING</b> route to advise (must be from application code, not test)
     * @param  advice    the AdviceWith configuration
     * @throws Exception if unable to apply advice or manage route lifecycle
     * @see              #isUseRouteBuilder()
     * @see              #createRouteBuilder()
     */
    protected void adviceRoute(String routeId, ThrowingConsumer<AdviceWithRouteBuilder, Exception> advice) throws Exception {
        // Check for Camel Quarkus advice antipattern
        if (createdRoutes != null && createdRoutes.contains(routeId)) {
            LOG.warn(
                    "AdviceWith detected on route '{}' which was created in from a test createRouteBuilder() override. This may cause unpredictable behavior."
                            +
                            "\nRefer to the Camel Quarkus testing guide AdviceWith examples section for more details."
                            +
                            "\nhttps://camel.apache.org/camel-quarkus/latest/reference/testing.html",
                    routeId);
        }

        context.getRouteController().stopRoute(routeId);
        AdviceWith.adviceWith(context, routeId, advice);
        context.getRouteController().startRoute(routeId);
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
    @Deprecated(since = "3.15.0")
    protected final MockEndpoint getMockEndpoint(String uri, boolean create) throws NoSuchEndpointException {
        return TestSupport.getMockEndpoint(context, uri, create);
    }

    /**
     * Single step debugs and Camel invokes this method before entering the given processor. This method is NOOP.
     *
     * @deprecated Use {@link #camelContextConfiguration()} to set an instance of {@link DebugBreakpoint}
     */
    @Deprecated(since = "3.15.0")
    protected void debugBefore(
            Exchange exchange, Processor processor, ProcessorDefinition<?> definition, String id, String label) {
    }

    /**
     * Single step debugs and Camel invokes this method after processing the given processor. This method is NOOP.
     *
     * @deprecated Use {@link #camelContextConfiguration()} to set an instance of {@link DebugBreakpoint}
     */
    @Deprecated(since = "3.15.0")
    protected void debugAfter(
            Exchange exchange, Processor processor, ProcessorDefinition<?> definition, String id, String label,
            long timeTaken) {
    }

    Set<String> getCreatedRoutes() {
        return createdRoutes;
    }

    Set<String> getExistingComponents() {
        return existingComponents;
    }

    /**
     * Restores routes to their original state by resetting outputs that were modified by AdviceWith.
     * This is called after each test to ensure advice doesn't persist across tests.
     */
    void restoreOriginalRouteDefinitions() {
        if (originalRouteOutputs == null || originalRouteOutputs.isEmpty()) {
            return;
        }

        ModelCamelContext modelContext = context.getCamelContextExtension().getContextPlugin(ModelCamelContext.class);
        if (modelContext == null) {
            return;
        }

        // Restore routes that have been modified by advice
        for (RouteDefinition routeDef : modelContext.getRouteDefinitions()) {
            String routeId = routeDef.getId();
            List<ProcessorDefinition<?>> originalOutputs = originalRouteOutputs.get(routeId);

            if (originalOutputs != null) {
                List<ProcessorDefinition<?>> currentOutputs = routeDef.getOutputs();
                boolean wasModified = false;

                // Check if the route was modified by comparing size or processor instances
                if (currentOutputs.size() != originalOutputs.size()) {
                    wasModified = true;
                } else {
                    // Same size, but check if processors were replaced
                    for (int i = 0; i < currentOutputs.size(); i++) {
                        if (currentOutputs.get(i) != originalOutputs.get(i)) {
                            wasModified = true;
                            break;
                        }
                    }
                }

                if (wasModified) {
                    // Check if user is advising a test-created route which is considered an antipattern on Camel Quarkus
                    if (createdRoutes != null && createdRoutes.contains(routeId)) {
                        LOG.warn(
                                "AdviceWith detected on route '{}' which was created in from a test createRouteBuilder() override. This may cause unpredictable behavior."
                                        +
                                        "\nRefer to the Camel Quarkus testing guide AdviceWith examples section for more details."
                                        +
                                        "\nhttps://camel.apache.org/camel-quarkus/latest/reference/testing.html",
                                routeId);
                    }

                    try {
                        // Stop the route before modifying it
                        context.getRouteController().stopRoute(routeId);

                        // Clear current outputs and restore original ones
                        currentOutputs.clear();
                        currentOutputs.addAll(originalOutputs);

                        // Restart the route
                        context.getRouteController().startRoute(routeId);
                    } catch (Exception e) {
                        LOG.warn("Failed to restore route '{}' after advice", routeId, e);
                    }
                }

            }
        }
    }

    private void assertTestClassCamelContextMatchesAppCamelContext() {
        // Test classes must use the same CamelContext as the application under test
        Assertions.assertEquals(context, contextManager.context(),
                "CamelQuarkusTestSupport uses a different CamelContext compared to the application under test");
    }

    @Override
    public void configureContext(CamelContextConfiguration camelContextConfiguration) {
        //to be overridden of child, if needed
    }

    @Override
    public void configureTest(TestExecutionConfiguration testExecutionConfiguration) {
        //to be overridden of child, if needed
    }
}
