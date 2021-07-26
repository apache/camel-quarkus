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
import java.util.concurrent.TimeoutException;

import org.apache.camel.quarkus.test.support.process.QuarkusProcessExecutor;
import org.apache.camel.util.StringHelper;
import org.assertj.core.api.Assertions;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessResult;

import static org.assertj.core.api.Assertions.assertThat;

public class CommandModeTest {

    //@Test
    void hello() throws InvalidExitValueException, IOException, InterruptedException, TimeoutException {
        final ProcessResult result = new QuarkusProcessExecutor("-Dgreeted.subject=Joe").execute();

        Assertions.assertThat(result.getExitValue()).isEqualTo(0);
        Assertions.assertThat(result.outputUTF8()).contains("Hello Joe!");
    }

    //@Test
    void testMainWarnsOnUnknownArguments() throws InterruptedException, IOException, TimeoutException {
        // Build a long fake classpath argument
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 150; i++) {
            builder.append("jar-" + i + ".jar:");
        }
        String classpathArg = builder.toString();

        final String[] jvmArgs = new String[] { "-Dgreeted.subject=Joe" };
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
        assertThat(result.outputUTF8()).contains("Hello Joe!");

        // Verify warning for unknown arguments was printed to the console
        String truncatedCpArg = String.format("%s...", StringHelper.limitLength(classpathArg, 97));
        assertThat(result.outputUTF8()).contains("Unknown option: -cp " + truncatedCpArg);
        assertThat(result.outputUTF8()).contains("Apache Camel Runner takes the following options");
    }
}
