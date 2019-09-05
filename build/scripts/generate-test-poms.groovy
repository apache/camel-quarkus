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
 * This script makes it possible to run Camel Quarkus integration tests outside of the current source tree, e.g inside
 * the planned Quarkus Platform project.
 *
 * The non-working stock solution:
 * Say that a third party project (call it 3PP) wants to run  Camel Quarkus integration tests within their build.
 * Camel Quarkus hopes to make that possible by providing a test-jar artifact for every integration test module.
 * 3PP hopes that adding one of our test-jars as a dependency in the module where they want to run our tests, will
 * result in surefire/failsafe Maven plugin to run those tests seamlessly. But the build will most probably fail due
 * dependencies missing in the class path of the executed tests. Closer look would reveal that the missing dependencies
 * are exactly the ones having the test scope in the pom of the Camel Quarkus integration test module. This is caused
 * by the fact that Maven, by design, doed not (transitively) pull test dependencied for you - see
 * https://issues.apache.org/jira/browse/MNG-1378
 *
 * The "ban test scope" solution:
 * Simply removing `<scope>test</scope>` from all dependencies on the side of Camel Quarkus would make the above
 * solution work. We do not like this solution because it pollutes the compile class path of the test application thus
 * prolonging the build of the native image.
 *
 * The "split tests from the test app" solution:
 * Another solution (on the Camel Quarkus side) is to put the test classes into `src/main/java` directory of a separate
 * Maven module. We do not like this solution because it makes our test projects non-idiomatic and hard to maintain.
 *
 * "Synthetic tests modules" solution:
 * That's what the present script implements. The script takes the POMs of all our integration test modules and does the
 * following modifications with them:
 *
 * * Renames the module by appending `-tests` to the original artifactId
 * * Removes all non-test dependencies
 * * Removes the test scope from every remaining dependency
 * * Adds the dependency on the original artifact (without the `-tests` suffix)
 *
 * The resulting pom.xml files are stored in the `target` directory of the calling maven module. The files are then used
 * by the maven-install-plugin and maven-deploy-plugin to install the test-jar under the new coordinates. So for the
 * third parties wanting to run our tests, this solution appears like the "split tests from the test app" solution.
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
