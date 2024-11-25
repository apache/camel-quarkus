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
import groovy.json.JsonOutput;
import groovy.json.JsonSlurper

import java.nio.file.Files
import java.nio.file.Paths

final int MAX_GROUPS = 2
final List<Map<String, String>> GROUPS = new ArrayList<>()
final String EXAMPLES_BRANCH = System.getProperty('EXAMPLES_BRANCH')
final String EXAMPLES_IGNORE= System.getProperty('EXAMPLES_IGNORE')

def ignoredExamples = []
if (EXAMPLES_IGNORE != null) {
    if (EXAMPLES_IGNORE.contains(",")) {
        ignoredExamples = EXAMPLES_IGNORE.split(",")
    } else if (Files.exists(Paths.get(EXAMPLES_IGNORE))) {
        ignoredExamples = Files.readAllLines(Paths.get(EXAMPLES_IGNORE))
    } else {
        ignoredExamples.add(EXAMPLES_IGNORE)
    }
}

int groupId = 0
JsonSlurper jsonSlurper = new JsonSlurper()

try {
    def url = new URL("https://raw.githubusercontent.com/apache/camel-quarkus-examples/${EXAMPLES_BRANCH}/docs/modules/ROOT/attachments/examples.json")
    def examples = jsonSlurper.parse(url)

    // Distribute example projects across a bounded set of test groups and output as JSON
    examples.each { example ->
        String projectName = example.link.substring(example.link.lastIndexOf('/') + 1)

        if (ignoredExamples.contains(projectName)) {
            return
        }

        if (GROUPS[groupId] == null) {
            GROUPS[groupId] = [:]
            GROUPS[groupId].name = "group-${String.format("%02d", groupId + 1)}"
            GROUPS[groupId].examples = ""
        }

        String separator = GROUPS[groupId].examples == "" ? "" : ","
        GROUPS[groupId].examples = "${GROUPS[groupId].examples}${separator}${projectName}"

        groupId += 1;
        if (groupId == MAX_GROUPS) {
            groupId = 0
        }
    }
} catch (Exception e) {
    // Ignored
}

print JsonOutput.toJson(["include": GROUPS])
