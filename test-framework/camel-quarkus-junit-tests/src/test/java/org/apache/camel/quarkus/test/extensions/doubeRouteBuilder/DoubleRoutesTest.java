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
package org.apache.camel.quarkus.test.extensions.doubeRouteBuilder;

import java.util.function.Supplier;

import io.quarkus.test.QuarkusDevModeTest;
import org.apache.camel.quarkus.test.extensions.CopyOfTestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

//Results:
//2026-02-18T17:34:30.1161300Z [INFO]
//2026-02-18T17:34:30.1161613Z [ERROR] Errors:
//2026-02-18T17:34:30.1163861Z [ERROR]   DoubleRoutesTest.checkTests:56 » ConditionTimeout Failed to wait for test run 1 State{lastRun=-1, running=true, inProgress=false, run=0, passed=0, failed=0, skipped=0, isBrokenOnly=false, isTestOutput=false, isInstrumentationBasedReload=false, isLiveReload=true}
//2026-02-18T17:34:30.1167441Z [ERROR]   ProducedRouteBuilderTest.checkTests:57 » ConditionTimeout Failed to wait for test run 1 State{lastRun=-1, running=true, inProgress=false, run=0, passed=0, failed=0, skipped=0, isBrokenOnly=false, isTestOutput=false, isInstrumentationBasedReload=false, isLiveReload=true}
//2026-02-18T17:34:30.1171496Z [ERROR]   RouteBuilderFailureTest.checkTests:53 » ConditionTimeout Failed to wait for test run 1 State{lastRun=-1, running=true, inProgress=false, run=0, passed=0, failed=0, skipped=0, isBrokenOnly=false, isTestOutput=false, isInstrumentationBasedReload=false, isLiveReload=true}
//2026-02-18T17:34:30.1175635Z [ERROR]   RouteBuilderWarningWithProducedBuilderTest.checkTests:52 » ConditionTimeout Failed to wait for test run 1 State{lastRun=-1, running=true, inProgress=false, run=0, passed=0, failed=0, skipped=0, isBrokenOnly=false, isTestOutput=false, isInstrumentationBasedReload=false, isLiveReload=true}
//2026-02-18T17:34:30.1179636Z [ERROR]   RouteBuilderWarningWithoutProducedBuilderTest.checkTests:51 » ConditionTimeout Failed to wait for test run 1 State{lastRun=-1, running=true, inProgress=false, run=0, passed=0, failed=0, skipped=0, isBrokenOnly=false, isTestOutput=false, isInstrumentationBasedReload=false, isLiveReload=true}
//2026-02-18T17:34:30.1182220Z [INFO]
//2026-02-18T17:34:30.1182620Z [ERROR] Tests run: 6, Failures: 0, Errors: 5, Skipped: 0
/**
 * Test for https://github.com/apache/camel-quarkus/issues/4560
 */
public class DoubleRoutesTest {

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class).addClass(RouteBuilder.class)
                            .add(new StringAsset(
                                    CopyOfTestUtil.appProperties("quarkus.naming.enable-jndi=true")),
                                    "application.properties");
                }
            })
            .setTestArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class).addClasses(FirstET.class, SecondET.class);
                }
            });

    @Test
    public void checkTests() {
        CopyOfTestUtil utils = new CopyOfTestUtil();
        CopyOfTestUtil.TestStatus ts = utils.waitForNextCompletion();

        Assertions.assertEquals(0L, ts.getTestsFailed());
        Assertions.assertEquals(4L, ts.getTestsPassed());
    }
}
