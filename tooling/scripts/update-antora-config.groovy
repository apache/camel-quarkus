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
 * Replace property values (defined in pom.xml files) in Antora yaml config
 */
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import java.util.stream.Stream
import java.util.stream.Collectors
import java.util.regex.Pattern
import java.util.regex.Matcher


final Path treeRootDir = Paths.get(properties['maven.multiModuleProjectDirectory'])

final List<Path> replaceInFiles = [
    treeRootDir.resolve('docs/antora.yml')
] as List

final List<Path> missingFiles = replaceInFiles.stream()
        .filter {path -> !Files.isRegularFile(path)}
        .collect(Collectors.toList())
if (!missingFiles.isEmpty()) {
    throw new IllegalStateException("Files expected to exist: " + missingFiles)
}


final Pattern replacementPattern = ~'([\\-\\:]) *([^ ]+) *# * replace \\$\\{([^}]+)\\}'

replaceInFiles.each { path ->
        println 'Updating ' + path
        final String content = path.getText('UTF-8')
        final Matcher m = replacementPattern.matcher(content)
        final StringBuffer newContent = new StringBuffer(content.length())
        while (m.find()) {
            final String property = m.group(3)
            final String newValue = project.properties.get(property)
            println " - replacing ${property} '" + m.group(2) +"' -> '${newValue}'"
            m.appendReplacement(newContent, '$1 ' + Matcher.quoteReplacement(newValue) + ' # replace ' + Matcher.quoteReplacement('${' + property + '}'))
        }
        m.appendTail(newContent)
        String newContentString = newContent.toString()

        // This can only work on main branch or during a release. Otherwise it will break antora.yml.
        if (path.getFileName().toString().equals('antora.yml')) {
            final String versionReplacement = 'version: ' + (project.version.endsWith('-SNAPSHOT') ? 'next' : project.version)
            println ' - setting '+ versionReplacement
            final Pattern versionPattern = ~'version: [^\\s]+'
            newContentString = versionPattern.matcher(newContentString).replaceFirst(versionReplacement)
        }

        if (!newContentString.equals(content)) {
            println 'Updated ' + path
            Files.write(path, newContentString.getBytes('UTF-8'))
        } else {
            println 'No change in ' + path
        }
}
