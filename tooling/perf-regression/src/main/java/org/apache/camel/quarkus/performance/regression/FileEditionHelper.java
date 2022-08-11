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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class FileEditionHelper {

    // We merely set the duration in the hyperfoil benchmark template
    public static void instantiateHyperfoilBenchmark(Path cqVersionUnderTestFolder, String singleScenarioDuration)
            throws IOException {
        File benchmarkFile = cqVersionUnderTestFolder.resolve("cq-perf-regression-scenario.hf.yaml").toFile();
        String benchmarkFileContent = FileUtils.readFileToString(benchmarkFile, StandardCharsets.UTF_8);
        benchmarkFileContent = benchmarkFileContent.replaceAll("372f6453-7527-43b1-850b-3824fc3d1187", singleScenarioDuration);
        FileUtils.writeStringToFile(benchmarkFile, benchmarkFileContent, StandardCharsets.UTF_8);
    }

    // We set the parent version and add staging repositories if needed
    public static void instantiatePomFile(Path cqVersionUnderTestFolder, String cqVersion, String cqStagingRepositoryUrl,
            String camelStagingRepositoryUrl)
            throws IOException, XmlPullParserException {
        File pomFile = cqVersionUnderTestFolder.resolve("pom.xml").toFile();

        try (FileReader fileReader = new FileReader(pomFile, StandardCharsets.UTF_8)) {
            MavenXpp3Reader pomReader = new MavenXpp3Reader();
            Model pomModel = pomReader.read(fileReader);

            pomModel.getParent().setVersion(cqVersion);

            if (cqStagingRepositoryUrl != null) {
                Repository cqStagingRepository = new Repository();
                cqStagingRepository.setId("camel-quarkus-staging");
                cqStagingRepository.setName("Camel Quarkus Staging Repository");
                cqStagingRepository.setUrl(cqStagingRepositoryUrl);
                pomModel.getPluginRepositories().add(cqStagingRepository);
                pomModel.getRepositories().add(cqStagingRepository);
            }
            if (camelStagingRepositoryUrl != null) {
                Repository camelStagingRepository = new Repository();
                camelStagingRepository.setId("camel-staging");
                camelStagingRepository.setName("Camel Staging Repository");
                camelStagingRepository.setUrl(camelStagingRepositoryUrl);
                pomModel.getPluginRepositories().add(camelStagingRepository);
                pomModel.getRepositories().add(camelStagingRepository);
            }

            try (FileWriter fileWriter = new FileWriter(pomFile, StandardCharsets.UTF_8)) {
                MavenXpp3Writer pomWriter = new MavenXpp3Writer();
                pomWriter.write(fileWriter, pomModel);
            }
        }
    }

}
