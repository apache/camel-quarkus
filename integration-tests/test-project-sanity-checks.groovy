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

import groovy.io.FileType

def badDeps = []
project.basedir.eachFile FileType.DIRECTORIES, { dir ->
    final File pomXml = new File(dir, "pom.xml")
    if (pomXml.exists()) {
        def pomXmlProject = new XmlParser().parseText(pomXml.getText('UTF-8'))
        pomXmlProject.dependencies.dependency
            .findAll { dep -> dep.scope.text() == 'test' }.stream()
            .map { dep -> "in "+ project.basedir.name +"/"+ dir.name +"/pom.xml : "+ dep.groupId.text() +":"+ dep.artifactId.text() }
            .each { key -> badDeps.add(key) }
    }
}
if (badDeps) {
    throw new RuntimeException("\nRemove <scope>test</scope> from the following dependencies:\n\n    "
        + badDeps.join("\n    ")
        + "\n\nThis is necessary to be able to build and run the test projects externally, e.g. inside Quarkus Platform")
}
