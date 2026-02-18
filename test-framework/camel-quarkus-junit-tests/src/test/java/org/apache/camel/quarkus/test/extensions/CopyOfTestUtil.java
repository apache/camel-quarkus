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
package org.apache.camel.quarkus.test.extensions;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.quarkus.dev.testing.ContinuousTestingSharedStateManager;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;

public class CopyOfTestUtil {
    long runToWaitFor = 1L;

    public CopyOfTestUtil() {
    }

    public TestStatus waitForNextCompletion() {
        try {
            Awaitility.waitAtMost(3L, TimeUnit.MINUTES).pollInterval(50L, TimeUnit.MILLISECONDS).until(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    ContinuousTestingSharedStateManager.State ts = ContinuousTestingSharedStateManager.getLastState();
                    if (ts.lastRun > runToWaitFor) {
                        runToWaitFor = ts.lastRun;
                    }

                    boolean runComplete = ts.lastRun == runToWaitFor;
                    if (runComplete && ts.inProgress) {
                        runToWaitFor = ts.lastRun + 1L;
                        return false;
                    } else {
                        if (runComplete) {
                            ++runToWaitFor;
                        }

                        return runComplete;
                    }
                }
            });
        } catch (Exception e) {
            ContinuousTestingSharedStateManager.State ts = ContinuousTestingSharedStateManager.getLastState();
            throw new ConditionTimeoutException("Failed to wait for test run " + this.runToWaitFor + " " + String.valueOf(ts),
                    e);
        }

        ContinuousTestingSharedStateManager.State s = ContinuousTestingSharedStateManager.getLastState();
        return new TestStatus(s.lastRun, s.running ? s.lastRun + 1L : -1L, s.run, s.currentPassed, s.currentFailed,
                s.currentSkipped, s.passed, s.failed, s.skipped);
    }

    public static String appProperties(String... props) {
        return "quarkus.test.continuous-testing=enabled\nquarkus.test.display-test-output=true\nquarkus.console.basic=true\nquarkus.console.disable-input=true\n"
                + String.join("\n", Arrays.asList(props));
    }

    public static class TestStatus {
        private long lastRun;
        private long running;
        private long testsRun = -1L;
        private long testsPassed = -1L;
        private long testsFailed = -1L;
        private long testsSkipped = -1L;
        private long totalTestsPassed = -1L;
        private long totalTestsFailed = -1L;
        private long totalTestsSkipped = -1L;

        public TestStatus() {
        }

        public TestStatus(long lastRun, long running, long testsRun, long testsPassed, long testsFailed, long testsSkipped,
                long totalTestsPassed, long totalTestsFailed, long totalTestsSkipped) {
            this.lastRun = lastRun;
            this.running = running;
            this.testsRun = testsRun;
            this.testsPassed = testsPassed;
            this.testsFailed = testsFailed;
            this.testsSkipped = testsSkipped;
            this.totalTestsPassed = totalTestsPassed;
            this.totalTestsFailed = totalTestsFailed;
            this.totalTestsSkipped = totalTestsSkipped;
        }

        public long getLastRun() {
            return this.lastRun;
        }

        public TestStatus setLastRun(long lastRun) {
            this.lastRun = lastRun;
            return this;
        }

        public long getRunning() {
            return this.running;
        }

        public TestStatus setRunning(long running) {
            this.running = running;
            return this;
        }

        public long getTestsRun() {
            return this.testsRun;
        }

        public TestStatus setTestsRun(long testsRun) {
            this.testsRun = testsRun;
            return this;
        }

        public long getTestsPassed() {
            return this.testsPassed;
        }

        public TestStatus setTestsPassed(long testsPassed) {
            this.testsPassed = testsPassed;
            return this;
        }

        public long getTestsFailed() {
            return this.testsFailed;
        }

        public TestStatus setTestsFailed(long testsFailed) {
            this.testsFailed = testsFailed;
            return this;
        }

        public long getTestsSkipped() {
            return this.testsSkipped;
        }

        public TestStatus setTestsSkipped(long testsSkipped) {
            this.testsSkipped = testsSkipped;
            return this;
        }

        public long getTotalTestsPassed() {
            return this.totalTestsPassed;
        }

        public TestStatus setTotalTestsPassed(long totalTestsPassed) {
            this.totalTestsPassed = totalTestsPassed;
            return this;
        }

        public long getTotalTestsFailed() {
            return this.totalTestsFailed;
        }

        public TestStatus setTotalTestsFailed(long totalTestsFailed) {
            this.totalTestsFailed = totalTestsFailed;
            return this;
        }

        public long getTotalTestsSkipped() {
            return this.totalTestsSkipped;
        }

        public TestStatus setTotalTestsSkipped(long totalTestsSkipped) {
            this.totalTestsSkipped = totalTestsSkipped;
            return this;
        }

        public String toString() {
            return "TestStatus{lastRun=" + this.lastRun + ", running=" + this.running + ", testsRun=" + this.testsRun
                    + ", testsPassed=" + this.testsPassed + ", testsFailed=" + this.testsFailed + ", testsSkipped="
                    + this.testsSkipped + "}";
        }
    }
}
