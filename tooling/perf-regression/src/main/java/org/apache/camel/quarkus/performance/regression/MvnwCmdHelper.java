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
package org.apache.camel.quarkus.performance.regression;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.environment.EnvironmentUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.io.output.TeeOutputStream;
import org.jboss.logging.Logger;

public class MvnwCmdHelper {

    private static final Logger LOGGER = Logger.getLogger(MvnwCmdHelper.class);

    public static String execute(Path cqVersionUnderTestFolder, String args) {

        ByteArrayOutputStream stdoutAndStderrMemoryStream = null;
        FileOutputStream stdoutFileStream = null;
        TeeOutputStream teeOutputStream = null;

        try {
            File mvnwFile = cqVersionUnderTestFolder.resolve("mvnw").toFile();
            CommandLine cmd = CommandLine.parse(mvnwFile.getAbsolutePath() + " " + args);

            stdoutAndStderrMemoryStream = new ByteArrayOutputStream();
            File logFile = cqVersionUnderTestFolder.resolve("logs.txt").toFile();
            stdoutFileStream = new FileOutputStream(logFile, true);

            stdoutFileStream.write("\n\n**********************************************************************\n"
                    .getBytes(StandardCharsets.UTF_8));
            stdoutFileStream.write("**********************************************************************\n"
                    .getBytes(StandardCharsets.UTF_8));
            stdoutFileStream.write(("** " + cmd + "\n").getBytes(StandardCharsets.UTF_8));
            stdoutFileStream.write("**********************************************************************\n"
                    .getBytes(StandardCharsets.UTF_8));
            stdoutFileStream.write("**********************************************************************\n"
                    .getBytes(StandardCharsets.UTF_8));

            teeOutputStream = new TeeOutputStream(stdoutAndStderrMemoryStream, stdoutFileStream);
            DefaultExecutor executor = new DefaultExecutor();
            PumpStreamHandler psh = new PumpStreamHandler(teeOutputStream);
            executor.setStreamHandler(psh);
            executor.setWorkingDirectory(cqVersionUnderTestFolder.toFile());

            Map<String, String> environment = EnvironmentUtils.getProcEnvironment();

            String newMavenOpts = "-Duser.language=en -Duser.country=US";
            if (environment.containsKey("MAVEN_OPTS")) {
                String currentMavenOpts = environment.get("MAVEN_OPTS");
                LOGGER.debugf("MAVEN_OPTS is already set up in the main process with value: %s", currentMavenOpts);
                newMavenOpts = currentMavenOpts + " " + newMavenOpts;
            }
            LOGGER.debugf("Setting MAVEN_OPTS in child process with value: %s", newMavenOpts);
            EnvironmentUtils.addVariableToEnvironment(environment, "MAVEN_OPTS=" + newMavenOpts);

            int exitValue = executor.execute(cmd, environment);
            String outAndErr = stdoutAndStderrMemoryStream.toString(StandardCharsets.UTF_8);
            if (exitValue != 0) {
                throw new RuntimeException("The command '" + cmd + "' has returned exitValue " + exitValue
                        + ", process logs below:\n" + outAndErr);
            }

            return outAndErr;
        } catch (IOException ex) {
            throw new RuntimeException("An issue occurred while attempting to execute 'mvnw " + args
                    + "', more logs may be found in " + cqVersionUnderTestFolder + "/logs.txt if exists", ex);
        } finally {
            IOUtils.closeQuietly(stdoutAndStderrMemoryStream);
            IOUtils.closeQuietly(stdoutFileStream);
            IOUtils.closeQuietly(teeOutputStream);
        }
    }

}
