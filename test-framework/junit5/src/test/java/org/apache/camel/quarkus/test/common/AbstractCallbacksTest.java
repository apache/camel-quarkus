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
package org.apache.camel.quarkus.test.common;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import io.quarkus.test.junit.callback.QuarkusTestContext;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import org.apache.camel.CamelContext;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.apache.camel.quarkus.test.junit5.patterns.DebugJUnit5Test;
import org.apache.camel.util.StopWatch;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public abstract class AbstractCallbacksTest extends CamelQuarkusTestSupport {

    private static final Logger LOG = Logger.getLogger(DebugJUnit5Test.class);

    public enum Callback {
        postTearDown,
        doSetup,
        preSetup,
        postSetup,
        contextCreation,
        afterAll,
        afterConstruct,
        afterEach,
        beforeEach;
    }

    private final String testName;
    private final String afterClassTestName;

    @Produce("direct:start")
    protected ProducerTemplate template;

    public AbstractCallbacksTest(String testName, String afterClassTestName) {
        this.testName = testName;
        this.afterClassTestName = afterClassTestName;
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        createTmpFile(testName, Callback.contextCreation);
        createTmpFile(afterClassTestName, Callback.contextCreation);
        return super.createCamelContext();
    }

    @Override
    protected void doPreSetup() throws Exception {
        createTmpFile(testName, Callback.preSetup);
        createTmpFile(afterClassTestName, Callback.preSetup);
        super.doPostSetup();
    }

    @Override
    protected void doSetUp() throws Exception {
        createTmpFile(testName, Callback.doSetup);
        createTmpFile(afterClassTestName, Callback.doSetup);
        super.doSetUp();
    }

    @Override
    protected void doPostSetup() throws Exception {
        createTmpFile(testName, Callback.postSetup);
        createTmpFile(afterClassTestName, Callback.postSetup);
        super.doPostSetup();
    }

    @Override
    protected void doPostTearDown() throws Exception {
        createTmpFile(testName, Callback.postTearDown);
        createTmpFile(afterClassTestName, Callback.postTearDown);
        super.doPostTearDown();
    }

    @Test
    public void testMock() throws Exception {
        getMockEndpoint("mock:result").expectedBodiesReceived("Hello World");
        template.sendBody("direct:start", "Hello World");
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testMock2() throws Exception {
        getMockEndpoint("mock:result").expectedBodiesReceived("Hello World 2");
        template.sendBody("direct:start", "Hello World 2");
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testMock3() throws Exception {
        getMockEndpoint("mock:result").expectedBodiesReceived("Hello World 2");
        template.sendBody("direct:start", "Hello World 2");
        assertMockEndpointsSatisfied();
    }

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:start").to("mock:result");
            }
        };
    }

    @Override
    protected void doAfterAll(QuarkusTestContext context) throws Exception {
        createTmpFile(testName, Callback.afterAll);
        createTmpFile(afterClassTestName, Callback.afterAll);
        super.doAfterAll(context);
    }

    @Override
    protected void doAfterConstruct() throws Exception {
        createTmpFile(testName, Callback.afterConstruct);
        createTmpFile(afterClassTestName, Callback.afterConstruct);
        super.doAfterConstruct();
    }

    @Override
    protected void doAfterEach(QuarkusTestMethodContext context) throws Exception {
        createTmpFile(testName, Callback.afterEach);
        createTmpFile(afterClassTestName, Callback.afterEach);
        super.doAfterEach(context);
    }

    @Override
    protected void doBeforeEach(QuarkusTestMethodContext context) throws Exception {
        createTmpFile(testName, Callback.beforeEach);
        createTmpFile(afterClassTestName, Callback.beforeEach);
        super.doAfterConstruct();
    }

    static void assertCount(int expectedCount, Long count, Callback c, String testName) {
        Assertions.assertEquals(expectedCount, count,
                c.name() + " should be called exactly " + expectedCount + " times in " + testName);
    }

    static void testAfterAll(String testName, BiConsumer<Callback, Long> consumer) {
        // we are called before doPostTearDown so lets wait for that to be
        // called
        Runnable r = () -> {
            Map<AbstractCallbacksTest.Callback, Long> counts = new HashMap<>();
            try {
                StopWatch watch = new StopWatch();
                while (watch.taken() < 5000) {
                    checkCallbacks(Callback.values(), testName, counts);

                    if (counts.size() == AbstractCallbacksTest.Callback.values().length) {
                        break;
                    } else {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            } finally {
                LOG.info("Should only call postTearDown 1 time per test class, called: ");
                for (Callback c : Callback.values()) {
                    consumer.accept(c, counts.get(c));
                }
            }

        };
        Thread t = new Thread(r);
        t.setDaemon(false);
        t.setName("shouldTearDown checker");
        t.start();
    }

    /**
     * Return -1 if there is no file. Numer of passed test otherwise.
     */
    public static int testFromAnotherClass(String testName, BiConsumer<Callback, Long> consumer) {
        int i = 0;
        Map<AbstractCallbacksTest.Callback, Long> counts = new HashMap<>();
        checkCallbacks(Callback.values(), testName, counts);
        if (counts.size() == 0) {
            return -1;
        }
        for (Callback c : Callback.values()) {
            consumer.accept(c, counts.get(c));
            i++;
        }
        return i;
    }

    private static void checkCallbacks(Callback[] values, String testName, Map<Callback, Long> counts) {
        LOG.debug("Checking for callbacks called correctly");
        try {
            for (Callback c : values) {
                long count = doesTmpFileExist(testName, c);
                if (count > 0) {
                    counts.put(c, count);
                }
            }
        } catch (Exception e) {
            //ignore
        }
    }

    private static void createTmpFile(String testName, Callback callback) throws Exception {
        Set<File> testDirs = Arrays.stream(Paths.get("target").toFile().listFiles())
                .filter(f -> f.isDirectory() && f.getName().startsWith(testName))
                .collect(Collectors.toSet());

        Path tmpDir;
        if (testDirs.size() == 1) {
            tmpDir = testDirs.stream().findFirst().get().toPath();
        } else if (testDirs.size() > 1) {
            throw new RuntimeException();
        } else {
            tmpDir = Files.createTempDirectory(Paths.get("target"), testName);
            tmpDir.toFile().deleteOnExit();
        }

        Path tmpFile = Files.createTempFile(tmpDir, callback.name(), ".log");
        tmpFile.toFile().deleteOnExit();
    }

    private static long doesTmpFileExist(String testName, Callback callback) throws Exception {
        //find test dir
        Set<File> testDirs = Arrays.stream(Paths.get("target").toFile().listFiles())
                .filter(f -> f.isDirectory() && f.getName().contains(testName))
                .collect(Collectors.toSet());
        if (testDirs.size() > 1) {
            LOG.warn("There are more tmp folders for the Callback tests.");
            return -1;
        }
        if (testDirs.isEmpty()) {
            LOG.warn("There is no tmp folder for the Callback tests.");
            return 0;
        }

        return Arrays.stream(testDirs.stream().findFirst().get().listFiles())
                .filter(f -> f.getName().startsWith(callback.name()))
                .count();
    }
}
