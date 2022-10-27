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
package org.apache.camel.quarkus.main.unknown.args.ignore;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import io.quarkus.test.junit.QuarkusTest;
import org.apache.camel.quarkus.test.support.process.QuarkusProcessExecutor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.zeroturnaround.exec.ProcessResult;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@Disabled("https://github.com/apache/camel-quarkus/issues/4218")
public class MainUnknownArgumentIgnoreTest {

    @Test
    public void testMainIgnoresUnknownArguments() throws InterruptedException, IOException, TimeoutException {
        final ProcessResult result = new QuarkusProcessExecutor(new String[] {}, "-d", "10", "-cp", "foo.jar", "-t").execute();

        // Verify the application ran successfully
        assertThat(result.getExitValue()).isEqualTo(0);
        assertThat(result.outputUTF8()).contains("Timer tick!");

        // Verify unknown arguments were ignored and no warning was printed to the console
        assertThat(result.outputUTF8()).doesNotContain("Unknown option: -cp foo.jar");
        assertThat(result.outputUTF8()).doesNotContain("Apache Camel Runner takes the following options");
    }
}
