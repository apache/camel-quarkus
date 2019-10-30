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

final String quarkusExtensionRelPath = 'runtime/src/main/resources/META-INF/quarkus-extension.yaml'
final List<String> messages = []

project.basedir.eachFile(FileType.DIRECTORIES) {
    if (new File(it, 'runtime/pom.xml').exists()) {
        final File extensionFile = new File(it, quarkusExtensionRelPath)
        final String shortPath = it.name + '/' + quarkusExtensionRelPath

        if (!extensionFile.exists()) {
            messages.add(shortPath + ' is missing')
        } else {
            def yaml = new org.yaml.snakeyaml.Yaml()
            def descriptor = yaml.load(extensionFile.getText("UTF-8"))

            if (!descriptor.name) {
                messages.add(shortPath + ' must contain name')
            }

            // metadata
            if (!descriptor.metadata) {
                messages.add(shortPath + ' must contain metadata section')
                return
            }
            if (!descriptor.metadata.guide?.equals('https://quarkus.io/guides/camel')) {
                messages.add(shortPath + ' must contain a link to the guide https://quarkus.io/guides/camel')
            }

            // keywords
            if (!descriptor.metadata.keywords) {
                messages.add(shortPath + ' must contain keywords section')
                return
            }
            if (!descriptor.metadata.keywords?.contains('camel')) {
                messages.add(shortPath + ' must contain a list of keywords with at least "camel" present')
            }
        }
    }
}

if (!messages.isEmpty()) {
    throw new RuntimeException("\nQuarkus extension metadata validation failures:\n\n    "
            + messages.join('\n    '))
}