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
import java.nio.file.Paths;
import java.util.concurrent.TimeoutException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

public class CommandModeTest {

    @Test
    void hello() throws InvalidExitValueException, IOException, InterruptedException, TimeoutException {

        final ProcessResult result = new ProcessExecutor()
                .command(command("Joe"))
                .readOutput(true)
                .execute();

        Assertions.assertThat(result.getExitValue()).isEqualTo(0);
        Assertions.assertThat(result.outputUTF8()).contains("Hello Joe!");

    }

    protected String[] command(String greetingSubject) {
        final boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");
        final String javaExecutable = System.getProperty("java.home") + (isWindows ? "/bin/java.exe" : "/bin/java");
        final String runnerJar = System.getProperty("quarkus.runner.jar");
        Assertions.assertThat(Paths.get(runnerJar)).exists();
        return new String[] { javaExecutable, "-Dgreeted.subject=" + greetingSubject, "-jar", runnerJar };
    }

}
