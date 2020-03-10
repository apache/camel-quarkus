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
import java.nio.file.Files
import java.nio.file.Path

final String[] extensionDirs = properties['extensionDirs'].split(',')

final String quarkusExtensionRelPath = 'runtime/src/main/resources/META-INF/quarkus-extension.yaml'
final List<String> messages = []
final Path basePath = project.basedir.toPath()

for (String extensionDir in extensionDirs) {
    final Path extensionDirPath = basePath.resolve(extensionDir)
    Files.list(extensionDirPath)
            .filter { path -> Files.isDirectory(path) }
            .filter { path -> Files.exists(path.resolve('runtime/pom.xml')) }
            .map { path -> path.resolve(quarkusExtensionRelPath) }
            .forEach { extensionFile ->
                final String shortPath = basePath.relativize(extensionFile).toString()
                if (!Files.exists(extensionFile)) {
                    messages.add(shortPath + ' is missing')
                } else {
                    def yaml = new org.yaml.snakeyaml.Yaml()
                    def descriptor = yaml.load(extensionFile.getText("UTF-8"))

                    if (!descriptor.name) {
                        messages.add(shortPath + ' must contain name')
                    }
                    if (!descriptor.description) {
                        messages.add(shortPath + ' must contain description')
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
                        messages.add(shortPath + ' metadata must contain keywords section')
                        return
                    }
                    if (!descriptor.metadata.keywords?.contains('camel')) {
                        messages.add(shortPath + ' metadata must contain a list of keywords with at least "camel" present')
                    }

                    // categories
                    if (!descriptor.metadata.categories) {
                        messages.add(shortPath + ' metadata must contain categories section')
                        return
                    }
                    if (!descriptor.metadata.categories?.contains('integration')) {
                        messages.add(shortPath + ' metadata must contain a list of categories with at least "integration" present')
                    }
                }
            }
}


if (!messages.isEmpty()) {
    throw new RuntimeException("\nQuarkus extension metadata validation failures:\n\n    "
            + messages.join('\n    '))
}