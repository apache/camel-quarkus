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
import static org.twdata.maven.mojoexecutor.MojoExecutor.Element
import static org.twdata.maven.mojoexecutor.MojoExecutor.*
import org.apache.maven.project.MavenProject
import org.apache.maven.execution.MavenSession
import org.apache.maven.model.Plugin

final String command = properties['itest.jar.command']

static boolean isItestModule(MavenProject project) {
    return 'jar'.equals(project.packaging) && new File(project.basedir, 'pom.xml').getText('UTF-8').contains('<goal>native-image</goal>')
}

static void install(MavenProject project, MavenSession session) {

    if (isItestModule(project)) {
        def pomXmlProject = new XmlParser().parseText(new File(project.basedir, 'pom.xml').getText('UTF-8'))
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

        final File outputPom = newPomPath(project, project.artifactId)
        outputPom.withWriter( 'UTF-8' ) { out ->
            final XmlNodePrinter printer = new XmlNodePrinter(new PrintWriter(out))
            printer.preserveWhitespace = true
            printer.print(pomXmlProject)
        }
        final File testJar = testJarPath(project, project.basedir, project.artifactId)

        Class bpmClass = Class.forName("org.apache.maven.plugin.BuildPluginManager");
        Object buildPluginManager = session.lookup(bpmClass.getName());
        executeMojo(
            managedPlugin("org.apache.maven.plugins", "maven-install-plugin", project),
            goal("install-file"),
            configuration(
                element("pomFile", outputPom.toString()),
                element("version", project.version),
                element("file", testJar.toString())
            ),
            executionEnvironment(
                project,
                session,
                buildPluginManager
            )
        )
    }

}

static Plugin managedPlugin(String groupId, String artifactId, MavenProject project) {
    for (p in project.build.plugins) {
        if (groupId.equals(p.groupId) && artifactId.equals(p.artifactId)) {
            println 'Resolved version ' + p.version + ' for ' + groupId +':'+artifactId
            return plugin(groupId, artifactId, p.version)
        }
    }
    throw new IllegalStateException('Could not find a managed version of '+ groupId + ':' + artifactId)
}


static File newPomPath(MavenProject project, String oldArtifactId) {
    return new File(project.basedir, 'target/'+ oldArtifactId + '-tests-pom.xml')
}
static File testJarPath(MavenProject project, File oldDir, String oldArtifactId) {
    return new File(oldDir, 'target/' + oldArtifactId + '-' + project.version + '-tests.jar')
}

static void deploy(MavenProject project, MavenSession session) {
    if (isItestModule(project)) {
        def distManagementRepo = project.version.endsWith('-SNAPSHOT') ? project.distributionManagement.snapshotRepository : project.distributionManagement.repository

        final File outputPom = newPomPath(project, project.artifactId)
        final File testJar = testJarPath(project, project.basedir, project.artifactId)

        Class bpmClass = Class.forName("org.apache.maven.plugin.BuildPluginManager");
        Object buildPluginManager = session.lookup(bpmClass.getName());

        List<Element> config = [];
        config.add(element("repositoryId", distManagementRepo.id))
        config.add(element("url", distManagementRepo.url))
        config.add(element("pomFile", outputPom.toString()))
        config.add(element("version", project.version))
        config.add(element("file", testJar.toString()))

        final File testSources = new File(testJar.toString().replace('.jar', '-sources.jar'))
        if (testSources.exists()) {
            config.add(element("sources", testSources.toString()))
        }
        final File testJavaDoc = new File(testJar.toString().replace('.jar', '-javadoc.jar'))
        if (testJavaDoc.exists()) {
            config.add(element("javadoc", testJavaDoc.toString()))
        }

        executeMojo(
            managedPlugin("org.apache.maven.plugins", "maven-deploy-plugin", project),
            goal("deploy-file"),
            configuration(config.toArray(new Element[config.size()])),
            executionEnvironment(
                project,
                session,
                buildPluginManager
            )
        )
    }
}


