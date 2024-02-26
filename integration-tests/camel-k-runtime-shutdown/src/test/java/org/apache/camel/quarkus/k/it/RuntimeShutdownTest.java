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
package org.apache.camel.quarkus.k.it;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.apache.camel.quarkus.test.support.process.QuarkusProcessExecutor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessResult;

@DisabledIfSystemProperty(named = "quarkus.runner", matches = ".*runner.jar", disabledReason = "https://github.com/apache/camel-quarkus/issues/4218")
public class RuntimeShutdownTest {

    @Test
    void testStopsAfterFirstMessage()
            throws InvalidExitValueException, IOException, InterruptedException, TimeoutException {

        QuarkusProcessExecutor executor = new QuarkusProcessExecutor(
                "-Dgreeted.subject=Joe",
                "-Dcamel.k.shutdown.max-messages=1",
                "-Dcamel.main.routes-include-pattern=classpath:routes/test-route-001.yaml");

        ProcessResult result = executor.execute();

        Assertions.assertThat(result.getExitValue()).isEqualTo(0);
        Assertions.assertThat(result.outputUTF8()).contains("Hello Joe!");
        Assertions.assertThat(result.outputUTF8()).contains("Once done");
        Assertions.assertThat(result.outputUTF8()).contains("Initiate runtime shutdown (max: 1, handled: 2)");
    }
}
