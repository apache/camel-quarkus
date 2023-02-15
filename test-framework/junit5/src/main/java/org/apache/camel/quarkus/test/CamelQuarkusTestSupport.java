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

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.callback.QuarkusTestContext;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.Service;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.quarkus.core.FastCamelContext;
import org.apache.camel.spi.Registry;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link CamelTestSupport} class does not work on Quarkus. This class provides a replacement, which can be used in
 * JVM mode. There are several differences between {@link CamelTestSupport} and this class.
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
public class CamelQuarkusTestSupport extends CamelTestSupport
        implements QuarkusTestProfile {

    private static final Logger LOG = LoggerFactory.getLogger(CamelQuarkusTestSupport.class);

    @Inject
    protected CamelContext context;

    /*
     * Set of routes, which were created by routeBuilder. This set is used by some callbacks.
     */
    Set<String> createdRoutes;

    //------------------------ quarkus callbacks ---------------

    /**
     * Replacement of {@link #afterAll(ExtensionContext)} called from {@link AfterAllCallback#afterAll(QuarkusTestContext)}
     */
    protected void doAfterAll(QuarkusTestContext context) throws Exception {
    }

    /**
     * Replacement of {@link #afterEach(ExtensionContext)} called from
     * {@link AfterEachCallback#afterEach(QuarkusTestMethodContext)}
     */
    protected void doAfterEach(QuarkusTestMethodContext context) throws Exception {
    }

    /**
     * Replacement of {@link #beforeAll(ExtensionContext)} called from {@link AfterConstructCallback#afterConstruct(Object)}
     * Execution differs in case of <i>@TestInstance(TestInstance.Lifecycle.PER_METHOD)</i> in which case callback is called
     * before each test (instead of {@link #beforeAll(ExtensionContext)}).
     */
    protected void doAfterConstruct() throws Exception {
    }

    /**
     * Replacement of {@link #beforeEach(ExtensionContext)} called from
     * {@link BeforeEachCallback#beforeEach(QuarkusTestMethodContext)}
     */
    protected void doBeforeEach(QuarkusTestMethodContext context) throws Exception {
    }

    /**
     * Feel free to override this method for the sake of customizing the instance returned by this implementation.
     * Do not create your own CamelContext instance, because there needs to exist just a single instance owned by
     * Quarkus CDI container. There are checks in place that will make your tests fail if you do otherwise.
     *
     * @return           The context from Quarkus CDI container
     * @throws Exception Overridden method has to throw the same Exception as superclass.
     */
    @Override
    protected CamelContext createCamelContext() throws Exception {
        return this.context;
    }

    /**
     * The same functionality as {@link CamelTestSupport#bindToRegistry(Registry)}.
     */
    @Override
    protected void bindToRegistry(Registry registry) throws Exception {
        //CamelTestSupport has to use the same context as CamelQuarkusTestSupport
        Assertions.assertEquals(context, super.context, "Different context found!");
        super.bindToRegistry(registry);
    }

    /**
     * The same functionality as {@link CamelTestSupport#postProcessTest()} .
     */
    @Override
    protected void postProcessTest() throws Exception {
        //CamelTestSupport has to use the same context as CamelQuarkusTestSupport
        Assertions.assertEquals(context, super.context, "Different context found!");
        super.postProcessTest();
    }

    /**
     * The same functionality as {@link CamelTestSupport#context()} .
     */
    @Override
    public CamelContext context() {
        //CamelTestSupport has to use the same context as CamelQuarkusTestSupport
        Assertions.assertEquals(context, super.context, "Different context found!");
        return super.context();
    }

    /**
     * This method is not called on Camel Quarkus because the `CamelRegistry` is created and owned by Quarkus CDI container.
     * If you need to customize the registry upon creation, you may want to override {@link #createCamelContext()}
     * in the following way:
     * 
     * <pre>
     * &#64;Override
     * protected CamelContext createCamelContext() throws Exception {
     *     CamelContext ctx = super.createCamelContext();
     *     Registry registry = ctx.getRegistry();
     *     // do something with the registry...
     *     return ctx;
     * }
     * </pre>
     * 
     * @return Never returns any result. UnsupportedOperationException is thrown instead.
     */
    @Override
    protected final Registry createCamelRegistry() {
        throw new UnsupportedOperationException("won't be executed.");
    }

    /**
     * This method does nothing. All necessary tasks are performed in
     * {@link BeforeEachCallback#beforeEach(QuarkusTestMethodContext)}
     * Use {@link #doAfterConstruct()} instead of this method.
     */
    @Override
    public final void beforeAll(ExtensionContext context) {
        //replaced by quarkus callback (beforeEach)
    }

    /**
     * This method does nothing. All tasks are performed in {@link BeforeEachCallback#beforeEach(QuarkusTestMethodContext)}
     * Use {@link #doBeforeEach(QuarkusTestMethodContext)} instead of this method.
     */
    @Override
    public final void beforeEach(ExtensionContext context) throws Exception {
        //replaced by quarkus callback (beforeEach)
    }

    /**
     * This method does nothing. All necessary tasks are performed in
     * {@link BeforeEachCallback#beforeEach(QuarkusTestMethodContext)}
     * Use {@link #doAfterAll(QuarkusTestContext)} instead of this method.
     */
    @Override
    public final void afterAll(ExtensionContext context) {
        //in camel-quarkus, junit5 uses different classloader, necessary code was moved into quarkus's callback
    }

    /**
     * This method does nothing. All necessary tasks are performed in
     * {@link BeforeEachCallback#beforeEach(QuarkusTestMethodContext)}
     * Use {@link #doAfterEach(QuarkusTestMethodContext)} instead of this method.
     */
    @Override
    public final void afterEach(ExtensionContext context) throws Exception {
        //in camel-quarkus, junit5 uses different classloader, necessary code was moved into quarkus's callback
    }

    /**
     * This method does nothing All necessary tasks are performed in
     * {@link BeforeEachCallback#beforeEach(QuarkusTestMethodContext)}
     * Use {@link #doAfterEach(QuarkusTestMethodContext)} instead of this method.
     */
    @Override
    public final void afterTestExecution(ExtensionContext context) throws Exception {
        //in camel-quarkus, junit5 uses different classloader, necessary code was moved into quarkus's callback
    }

    /**
     * Method {@link CamelTestSupport#setUp()} is triggered via annotation {@link org.junit.jupiter.api.BeforeEach}.
     * Its execution is disabled (by using overriding method without any annotation) and is executed from
     * {@link BeforeEachCallback}
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Method {@link CamelTestSupport#tearDown()} is triggered via annotation
     * {@link org.junit.jupiter.api.AfterEach}.
     * Its execution is disabled (by using overriding method without any annotation) and is executed from
     * {@link AfterEachCallback}
     */
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * This method stops the Camel context. Be aware that on of the limitation that Quarkus brings is that context
     * can not be started (lifecycle f the context is bound to the application) .
     *
     * @throws Exception
     */
    @Override
    protected void stopCamelContext() throws Exception {
        //context is started and stopped via quarkus lifecycle
    }

    /**
     * Allows running of the CamelTestSupport child in the Quarkus application.
     * Method is not intended to be overridden.
     */
    @Override
    protected final void doQuarkusCheck() {
        //can run on Quarkus

        //log warning in case that at least one RouteBuilder in the registry, it might mean, that unintentionally
        // RouteBuilders are shared across or that RouteBuilder is created with @Produces
        if (isUseRouteBuilder() && !context.getRegistry().findByType(RouteBuilder.class).isEmpty()) {
            LOG.warn(
                    "Test with `true` in `isUserRouteBuilder' and `RouteBuilder` detected in the context registry. " +
                            "All tests will share this routeBuilder from the registry. This is usually not intended. " +
                            "If `@Produces` is used to create such a RouteBuilder, please refactor the code " +
                            "by overriding the method `createRouteBuilder()` instead.");
        }
    }

    void internalAfterAll(QuarkusTestContext context, ExtensionContext extensionContext) {
        try {
            if (isCreateCamelContextPerClass()) {
                super.afterAll(extensionContext);
            } else {
                doPostTearDown();
            }
            cleanupResources();

        } catch (Exception e) {
            // ignore
        }
    }

    void internalBeforeAll(ExtensionContext context) {
        super.beforeAll(context);
    }

    void internalBeforeEach(ExtensionContext context) throws Exception {
        super.beforeEach(context);
    }

    /**
     * Strategy to perform any pre setup, before {@link CamelContext} is created
     * <p>
     * Be aware that difference in lifecycle with Quarkus may require a special behavior.
     * If this method is overridden, <i>super.doPreSetup()</i> has to be called.
     * </p>
     */
    @Override
    protected void doPreSetup() throws Exception {
        if (isUseAdviceWith() || isUseDebugger()) {
            ((FastCamelContext) context).suspend();
        }

        if (isUseRouteBuilder()) {
            //save the routeIds of routes existing before setup
            createdRoutes = context.getRoutes().stream().map(r -> r.getRouteId()).collect(Collectors.toSet());
        }

        super.doPreSetup();
    }

    /**
     * Strategy to perform any post setup after {@link CamelContext} is created
     * <p>
     * Be aware that difference in lifecycle with Quarkus may require a special behavior.
     * If this method is overridden, <i>super.doPostSetup()</i> has to be called.
     * </p>
     */
    @Override
    protected void doPostSetup() throws Exception {
        if (isUseAdviceWith() || isUseDebugger()) {
            ((FastCamelContext) context).resume();
            if (isUseDebugger()) {
                ModelCamelContext mcc = context.adapt(ModelCamelContext.class);
                List<RouteDefinition> rdfs = mcc.getRouteDefinitions();
                //if context was suspended routes was not added, because it would trigger start of the context
                // routes have to be added now
                mcc.addRouteDefinitions(rdfs);
            }
        }

        if (isUseRouteBuilder()) {
            //remove from the routes all routes which existed before setup
            var allRoutes = context.getRoutes().stream().map(r -> r.getRouteId()).collect(Collectors.toSet());
            if (createdRoutes != null) {
                allRoutes.removeAll(createdRoutes);
            }
            createdRoutes = allRoutes;
        }
        super.doPostSetup();
    }

    /**
     * Internal disablement of the context stop functionality.
     */
    @Override
    protected final void doStopCamelContext(CamelContext context, Service camelContextService) {
        //don't stop
    }

    /**
     * This method does nothing. The context starts together with Quarkus engine.
     */
    @Override
    protected final void startCamelContext() {
        //context has already started
    }

    /**
     * Override when using <a href="http://camel.apache.org/advicewith.html">advice with</a> and return <tt>true</tt>.
     * This helps knowing advice with is to be used.
     * <p/>
     * <b>Important:</b> Its important to execute method {@link #startRouteDefinitions()}} manually from the unit test
     * after you are done doing all the advice with.
     *
     * @return <tt>true</tt> if you use advice with in your unit tests.
     */
    @Override
    public boolean isUseAdviceWith() {
        return false;
    }

    /**
     * Helper method to start routeDefinitions (to be used with `adviceWith`).
     */
    protected void startRouteDefinitions() throws Exception {
        List<RouteDefinition> definitions = new ArrayList<>(context.adapt(ModelCamelContext.class).getRouteDefinitions());
        for (Route r : context.getRoutes()) {
            //existing route does not need to be started
            definitions.remove(r.getRoute());
        }
        context.adapt(ModelCamelContext.class).startRouteDefinitions(definitions);
    }

}
