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
import groovy.json.JsonSlurper

final String quarkusExtensionJsonRelPath = 'runtime/src/main/resources/META-INF/quarkus-extension.json'
final List<String> messages = []
project.basedir.eachFile FileType.DIRECTORIES, { extensionParentDir ->
    if (new File(extensionParentDir, 'runtime/pom.xml').exists()) {
        final File extensionJsonFile = new File(extensionParentDir, quarkusExtensionJsonRelPath)
        final String shortName = extensionParentDir.getName()
        final String shortPath = shortName + '/' + quarkusExtensionJsonRelPath
        final boolean internal = (shortName.startsWith('core') || shortName.endsWith('-common'))
        if (!extensionJsonFile.exists()) {
            messages.add(shortPath + ' is missing')
        } else {
            final Map extensionJson = new JsonSlurper().parseText(extensionJsonFile.getText("UTF-8"))
            if (extensionJson['name'] == null) {
                messages.add(shortPath + ' must contain name')
            }
            if (!(extensionJson['labels'] instanceof List)
                    || !extensionJson['labels'].contains("camel")
                    || !extensionJson['labels'].contains("integration")) {
                messages.add(shortPath + ' must contain a list of labels with at least "integration" and "camel" labels present')
            }
            if (extensionJson['guide'] == null || !"https://quarkus.io/guides/camel".equals(extensionJson['guide'])) {
                messages.add(shortPath + ' must contain a link to the guide https://quarkus.io/guides/camel')
            }
        }
    }
}

if (!messages.isEmpty()) {
    throw new RuntimeException("\nQuarkus extension metadata validation failures:\n\n    "
            + messages.join("\n    "))
}