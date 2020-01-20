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
import java.nio.file.Path
import java.nio.file.Paths

final List<String> badDeps = []
final List<String> parityViolations = []
final File pomXml = new File(project.basedir, "pom.xml")

/* groupIds that contain extensions */
final Set<String> extensionGroupIds = ["org.apache.camel.quarkus", "io.quarkus"] as Set
/* artifactIds from groups contained in extensionGroupIds that are not extensions */
final Set<String> nonExtensionArtifactIds = [] as Set

final Path treeRootDir = Paths.get(project.properties['camel.quarkus.project.root'])
final Path relativePomPath = treeRootDir.relativize(pomXml.toPath().normalize())

if (pomXml.exists()) {
    def pomXmlProject = new XmlParser().parseText(pomXml.getText('UTF-8'))
    pomXmlProject.dependencies.dependency
        .findAll {
            !it.version.text().isEmpty()
        }
        .each {
            badDeps << "in ${relativePomPath} : ${it.groupId.text()}:${it.artifactId.text()}"
        }

    /* Enforce the dependency parity between runtime and deployment modules */
    final String deploymentArtifactId = pomXmlProject.artifactId.text()
    if (isDeploymentArtifactId(deploymentArtifactId)) {
        final String runtimeArtifactId = toRuntimeArtifactId(deploymentArtifactId)

        if (pomXmlProject.dependencies.dependency.findAll { runtimeArtifactId.equals(it.artifactId.text()) }.size() == 0) {
            parityViolations << "${relativePomPath} must depend on ${runtimeArtifactId}"
        }

        final Set<Tuple2> expectedRuntimeDeps = [] as LinkedHashSet
        pomXmlProject.dependencies.dependency
            .findAll {
                isDeploymentArtifactId(it.artifactId.text()) && !it.scope
            }
            .each {
                expectedRuntimeDeps.add(new Tuple2(it.groupId.text(), toRuntimeArtifactId(it.artifactId.text())))
            }

        final Set<Tuple2> actualRuntimeDeps = [] as LinkedHashSet
        final File runtimePomXml = new File(project.basedir, "../runtime/pom.xml")

        final Path relativeRuntimePomPath = treeRootDir.relativize(runtimePomXml.toPath().toAbsolutePath().normalize())

        def runtimeProject = new XmlParser().parseText(runtimePomXml.getText('UTF-8'))
        runtimeProject.dependencies.dependency
            .findAll {
                extensionGroupIds.contains(it.groupId.text()) &&
                    !nonExtensionArtifactIds.contains(it.artifactId.text()) &&
                    !it.scope
            }
            .each {
                actualRuntimeDeps.add(new Tuple2(it.groupId.text(), it.artifactId.text()))
            }

        // println "expectedRuntimeDeps: " + expectedRuntimeDeps
        // println "actualRuntimeDeps:   " + actualRuntimeDeps

        expectedRuntimeDeps
            .findAll {
                !actualRuntimeDeps.contains(it)
            }
            .each {
                parityViolations << "${relativeRuntimePomPath}     is missing  ${it.first}:${it.second}  dependency?"
            }

        actualRuntimeDeps
            .findAll {
                !expectedRuntimeDeps.contains(it)
            }
            .each {
                parityViolations << "${relativePomPath}  is missing  ${it.first}:${it.second}-deployment  dependency?"
            }

    }
}

if (!badDeps.isEmpty() || !parityViolations.isEmpty()) {
    final StringBuilder msg = new StringBuilder()
    if (!badDeps.isEmpty()) {
        msg.append("\nRemove explicit version from the following dependencies and rather manage them in one of the BOMs:\n\n    "
                + badDeps.join("\n    "))
    }
    if (!parityViolations.isEmpty()) {
        msg.append("\nViolations in the parity between deployment module dependencies and runtime module dependencies:\n\n    "
                + parityViolations.join("\n    "))
    }
    throw new RuntimeException(msg.toString())
}

boolean isDeploymentArtifactId(String artifactId) {
    return artifactId.endsWith("-deployment")
}
String toRuntimeArtifactId(String deploymentArtifactId) {
    return deploymentArtifactId.substring(0, deploymentArtifactId.length() - "-deployment".length())
}