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
package org.apache.camel.quarkus.performance.regression.it;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public class PerfRegressionIT {

    @Test
    void noArgsShouldPrintHelp() throws IOException, InterruptedException, TimeoutException {
        try {
            String processOutput = new ProcessExecutor()
                    .command("java", "-jar", "target/quarkus-app/quarkus-run.jar")
                    .readOutput(true)
                    .exitValue(2)
                    .execute()
                    .outputUTF8();

            assertThat(processOutput, containsString("Missing required parameter: '<versions>'"));
            assertThat(processOutput, containsString("-an, --also-run-native-mode"));
            assertThat(processOutput, containsString("-cqs, --camel-quarkus-staging-repository=<cqStagingRepository>"));
            assertThat(processOutput, containsString("-cs, --camel-staging-repository=<camelStagingRepository>"));
            assertThat(processOutput, containsString("-d, --duration=<singleScenarioDuration>"));
        } catch (InvalidExitValueException ievex) {
            fail("The perf-regression process has finished with an unexpected exit value", ievex);
        }
    }

    @Test
    void nominalShouldPrintReport() throws IOException, InterruptedException, TimeoutException {

        try {
            String cqVersion = System.getProperty("camel.quarkus.version");

            String processOutput = new ProcessExecutor()
                    .command("java", "-jar", "target/quarkus-app/quarkus-run.jar", "-d", "1s", cqVersion)
                    .readOutput(true)
                    .exitValue(0)
                    .execute()
                    .outputUTF8();

            String reportSummary = "Camel Quarkus Throughput Performance Increase Compared to Previous Version";
            assertThat(processOutput, containsString(reportSummary));

            String reportAndStopLogs = StringUtils.substringAfter(processOutput, reportSummary);
            assertNotNull(reportAndStopLogs);
            String[] reportAndStopLines = reportAndStopLogs.split(System.lineSeparator());
            assertThat(reportAndStopLines.length, greaterThanOrEqualTo(4));

            String titleLine = reportAndStopLines[1];
            assertThat(titleLine, containsString(" JVM req/s [%increase] "));

            String reportLine = reportAndStopLines[3];
            assertThat(reportLine, containsString(" " + cqVersion + " "));
            assertThat(reportLine, containsString(" 1s "));
            assertThat(reportLine, containsString(" req/s [+0.00%] "));
            assertThat(reportLine, containsString(" OK "));
        } catch (InvalidExitValueException ievex) {
            fail("The perf-regression process has finished with an unexpected exit value", ievex);
        }
    }

}
