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
package org.apache.camel.quarkus.test.support.process;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.apache.camel.quarkus.test.AvailablePortFinder;
import org.jboss.logging.Logger;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.StartedProcess;

/**
 * Support class for executing a Quarkus executable JAR or native executable with arbitrary arguments
 */
public class QuarkusProcessExecutor {

    private static final Logger LOGGER = Logger.getLogger(QuarkusProcessExecutor.class);
    private final ProcessExecutor executor;
    private final int httpPort = AvailablePortFinder.getNextAvailable();
    private final int httpsPort = AvailablePortFinder.getNextAvailable();

    public QuarkusProcessExecutor(String... jvmArgs) {
        this(jvmArgs, (String[]) null);
    }

    public QuarkusProcessExecutor(String[] jvmArgs, String... applicationArgs) {
        this(null, jvmArgs, applicationArgs);
    }

    public QuarkusProcessExecutor(Consumer<ProcessExecutor> customizer, String[] jvmArgs, String... applicationArgs) {
        List<String> command = command(jvmArgs);
        if (applicationArgs != null) {
            command.addAll(Arrays.asList(applicationArgs));
        }

        LOGGER.infof("Executing process: %s", String.join(" ", command));
        executor = new ProcessExecutor()
                .command(command)
                .redirectOutput(System.out)
                .readOutput(true);

        if (customizer != null) {
            customizer.accept(executor);
        }
    }

    public ProcessResult execute() throws InterruptedException, TimeoutException, IOException {
        return executor.execute();
    }

    public StartedProcess start() throws IOException {
        return executor.start();
    }

    public int getHttpPort() {
        return httpPort;
    }

    public int getHttpsPort() {
        return httpsPort;
    }

    protected List<String> command(String... args) {
        final boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");
        final String javaExecutable = System.getProperty("java.home") + (isWindows ? "/bin/java.exe" : "/bin/java");
        String runner = System.getProperty("quarkus.runner");

        if (runner == null) {
            throw new IllegalStateException("The quarkus.runner system property is not set");
        }

        if (!Paths.get(runner).toFile().exists()) {
            throw new IllegalStateException("Quarkus application runner does not exist: " + runner);
        }

        List<String> runnerArgs = new ArrayList<>();
        if (runner.endsWith(".jar")) {
            runnerArgs.add(javaExecutable);
            runnerArgs.addAll(Arrays.asList(args));
            runnerArgs.add("-Dquarkus.http.port=" + httpPort);
            runnerArgs.add("-Dquarkus.http.ssl-port=" + httpsPort);
            runnerArgs.add("-jar");
            runnerArgs.add(runner);
        } else {
            runner += (isWindows ? ".exe" : "");
            runnerArgs.add(runner);
            runnerArgs.addAll(Arrays.asList(args));
            runnerArgs.add("-Dquarkus.http.port=" + httpPort);
            runnerArgs.add("-Dquarkus.http.ssl-port=" + httpsPort);
        }

        return runnerArgs;
    }
}
