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

/**
 * When wanting to run integration tests using test jars as installed/deployed by Maven defaults the dependencies with
 * scope test are missing. To solve that we install/deploy those same jars under a separate artifactId with only test
 * deps there (without the test scope) and with the original artifact added as a dependency. This mimics the setup
 * (officially recommended by Maven) where the test classes are in a separate module from the test application.
 */

import groovy.io.FileType

new File(project.basedir, 'target').mkdirs()

project.basedir.eachFile FileType.DIRECTORIES, { dir ->
    final File pomXml = new File(dir, 'pom.xml')
    if (pomXml.exists()) {
        def pomXmlProject = new XmlParser().parseText(pomXml.getText('UTF-8'))
        final String oldArtifactId = pomXmlProject.artifactId.text()
        final String newArtifactId = oldArtifactId + '-tests'
        pomXmlProject.artifactId[0].value = newArtifactId

        pomXmlProject.name.each { n -> n.value = n.text() + ' :: Tests' }
        pomXmlProject.description.each { n -> n.parent().remove(n) }

        pomXmlProject.dependencies.dependency.each { dep ->
            if (dep.scope.text() == 'test') {
                dep.remove(dep.scope)
            } else {
                dep.parent().remove(dep)
            }
        }
        final Node dep = new Node(pomXmlProject.dependencies[0], 'dependency')
        new Node(dep, 'artifactId', [:], oldArtifactId)
        new Node(dep, 'groupId', [:], project.groupId)
        new Node(dep, 'version', [:], project.version)

        pomXmlProject.build.each { n -> n.parent().remove(n) }
        pomXmlProject.profiles.each { n -> n.parent().remove(n) }

        new File(project.basedir, 'target/'+ newArtifactId + '-pom.xml').withWriter( 'UTF-8' ) { out ->
            final XmlNodePrinter printer = new XmlNodePrinter(new PrintWriter(out))
            printer.preserveWhitespace = true
            printer.print(pomXmlProject)
        }
    }
}
