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
package org.apache.camel.quarkus.main.cmd.it;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.camel.quarkus.test.support.process.QuarkusProcessExecutor;
import org.apache.camel.util.StringHelper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.StartedProcess;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("https://github.com/apache/camel-quarkus/issues/4218")
public class CommandModeTest {

    @Test
    void testMainStopsAfterFirstMessage()
            throws InvalidExitValueException, IOException, InterruptedException, TimeoutException {
        final ProcessResult result = new QuarkusProcessExecutor("-Dgreeted.subject=Joe", "-Dcamel.main.duration-max-messages=1")
                .execute();

        Assertions.assertThat(result.getExitValue()).isEqualTo(0);
        Assertions.assertThat(result.outputUTF8()).contains("Logging Hello Joe! - from timer named hello");
        Assertions.assertThat(result.outputUTF8()).contains("Duration max messages triggering shutdown of the JVM");
    }

    @Test
    void testMainWarnsOnUnknownArguments() throws InterruptedException, IOException, TimeoutException {
        // Build a long fake classpath argument
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 150; i++) {
            builder.append("jar-" + i + ".jar:");
        }
        String classpathArg = builder.toString();

        final String[] jvmArgs = new String[] { "-Dgreeted.subject=Jane", "-Dcamel.main.duration-max-messages=1" };
        final String[] applicationArgs = new String[] {
                "-d",
                "10",
                "-cp",
                classpathArg,
                "-t"
        };

        final ProcessResult result = new QuarkusProcessExecutor(jvmArgs, applicationArgs).execute();

        // Verify the application ran successfully
        assertThat(result.getExitValue()).isEqualTo(0);
        Assertions.assertThat(result.outputUTF8()).contains("Logging Hello Jane! - from timer named hello");
        Assertions.assertThat(result.outputUTF8()).contains("Duration max messages triggering shutdown of the JVM");

        // Verify warning for unknown arguments was printed to the console
        String truncatedCpArg = String.format("%s...", StringHelper.limitLength(classpathArg, 97));
        assertThat(result.outputUTF8()).contains("Unknown option: -cp " + truncatedCpArg);
        assertThat(result.outputUTF8()).contains("Apache Camel Runner takes the following options");
    }

    @Test
    void testMainStopsAfterMaxSeconds() throws IOException, InterruptedException, ExecutionException {
        final StartedProcess process = new QuarkusProcessExecutor("-Dgreeted.subject=Jade",
                "-Dcamel.main.duration-max-seconds=3").start();
        try {
            ProcessResult result = process.getFuture().get(10, TimeUnit.SECONDS);
            Assertions.assertThat(result.getExitValue()).isEqualTo(0);
            Assertions.assertThat(result.outputUTF8()).contains("Waiting until complete: Duration max 3 seconds");
            Assertions.assertThat(result.outputUTF8()).contains("Logging Hello Jade! - from timer named hello");
            Assertions.assertThat(result.outputUTF8()).contains("Duration max seconds triggering shutdown of the JVM");
        } catch (TimeoutException e) {
            Assertions.fail("The process should not take so long as camel.main.duration-max-seconds is set");
        }
    }
}
