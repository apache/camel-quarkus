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
final File pomXml = new File(project.basedir, "pom.xml")

/* groupIds that contain extensions */
final Set<String> extensionGroupIds = ["org.apache.camel.quarkus", "io.quarkus", "org.amqphub.quarkus"] as Set
/* artifactIds from groups contained in extensionGroupIds that are not extensions */
final Set<String> nonExtensionArtifactIds = ["quarkus-development-mode-spi", "camel-quarkus-qute-component"] as Set

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

}

if (!badDeps.isEmpty()) {
    final StringBuilder msg = new StringBuilder()
    if (!badDeps.isEmpty()) {
        msg.append("\nRemove explicit version from the following dependencies and rather manage them in one of the BOMs:\n\n    "
                + badDeps.join("\n    "))
    }
    throw new RuntimeException(msg.toString())
}
