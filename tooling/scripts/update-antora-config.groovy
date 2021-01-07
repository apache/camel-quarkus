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
 * Makes sure that each itest is executed by the CI
 */
import java.nio.file.Path
import java.nio.file.Files
import java.util.stream.Stream
import java.util.regex.Pattern


final Path treeRootDir = Paths.get(properties['maven.multiModuleProjectDirectory'])

final final List<Path> replaceInFiles = [
    treeRootDir.resolve('docs/antora-playbook.yml'),
    treeRootDir.resolve('docs/antora-playbook-dev.yml'),
    treeRootDir.resolve('docs/antora.yml')
] as List

final List<Path> missingFiles = replaceInFiles.stream()
        .filter {path -> !Files.isRegularFile(path)}
        .collect(Collectors.toList())
if (!missingFiles.isEmpty()) {
    throw new IllegalStateException("Files expected to exist: " + missingFiles)
}

final Pattern replacementPattern = Pattern.compile("([\\-\\:]) *([^ ]+) *# * replace ${([^}]+)}")

replaceInFiles.stream()
    .forEach { path ->
        final String content = path.getText('UTF-8')
        final Matcher m = replacementPattern.matcher(content)
        final StringBuffer newContent = new StringBuffer(content.length())
        while (m.find()) {
            final String property = m.group(3)
            final String newValue = properties.get(property)
            m.appendReplacement(newContent, '$1 ' + Matcher.quoteReplacement(newValue) + ' # replace '" + Matcher.quoteReplacement('${' + property + '}'))
        }
        m.appendTail(newContent)
        final String newContentString = newContent.toString()
        if (!newContentString.equals(content)) {
            Files.write(path, newContentString.getBytes('UTF-8'))
        }
    }
