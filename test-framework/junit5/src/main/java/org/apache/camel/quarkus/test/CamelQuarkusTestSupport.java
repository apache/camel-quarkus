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

import java.util.List;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.callback.QuarkusTestContext;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import org.apache.camel.CamelContext;
import org.apache.camel.Service;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.quarkus.core.FastCamelContext;
import org.apache.camel.spi.Registry;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * The {@link CamelTestSupport} class does not work on Quarkus. This class provides a replacement, which can be used in
 * JVM mode.
 * <p>
 * There are several differences between {@link CamelTestSupport} and this class:
 * <ul>
 * <li>Starting and stopping `CamelContext` in Camel Quarkus is generally bound to starting and stopping the application
 * and this holds also when testing.</li>
 * <li>Starting and stopping the application under test (and thus also `CamelContext`) is under full control of Quarkus
 * JUnit Extension. It prefers keeping the application up and running unless it is told to do otherwise.</li>
 * <li>Hence normally the application under test is started only once for all test classes of the given Maven/Gradle
 * module.</li>
 * <li>To force Quarkus JUnit Extension to restart the application (and thus also `CamelContext`) for a given test
 * class, you need to assign a unique `@io.quarkus.test.junit.TestProfile` to that class. Check the
 * https://quarkus.io/guides/getting-started-testing#testing_different_profiles[Quarkus documentation] how you can do
 * that. (Note that
 * `https://quarkus.io/guides/getting-started-testing#quarkus-test-resource[@io.quarkus.test.common.QuarkusTestResource]`
 * has a similar effect.)</li>
 * <li>Camel Quarkus executes the production of beans during the build phase. Because all the tests are
 * build together, exclusion behavior is implemented into `CamelQuarkusTestSupport`. If a producer of the specific type
 * and name is used in one tests, the instance will be the same for the rest of the tests.</li>
 * <li>Unit Jupiter callbacks (`BeforeEachCallback`, `AfterEachCallback`, `AfterAllCallback`, `BeforeAllCallback`,
 * `BeforeTestExecutionCallback` and `AfterTestExecutionCallback`) might not work correctly. See the
 * <a href="https://quarkus.io/guides/getting-started-testing#enrichment-via-quarkustestcallback">documentation</a>.
 * Methods `afterAll`, `afterEach`, `afterTestExecution`, `beforeAll` and `beforeEach` are not executed anymore.
 * You should use `doAfterAll`, `doAfterConstruct`, `doAfterEach`, `doBeforeEach` and `doBeforeAll` instead of
 * them.</li>
 * </ul>
 * </p>
 */
public class CamelQuarkusTestSupport extends CamelTestSupport
        implements QuarkusTestProfile {

    //Flag, whether routes was created by test's route builder and therefore should be stopped and removed based on lifecycle
    private boolean wasUsedRouteBuilder;

    @Inject
    protected CamelContext context;

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
     * @Override
     *           protected CamelContext createCamelContext() throws Exception {
     *           CamelContext ctx = super.createCamelContext();
     *           Registry registry = ctx.getRegistry();
     *           // do something with the registry...
     *           return ctx;
     *           }
     *
     * @return   Never returns any result. UnsupportedOperationException is thrown instead.
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
    }

    void internalAfterAll(QuarkusTestContext context) {
        try {
            doPostTearDown();
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
}
