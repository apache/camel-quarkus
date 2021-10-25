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

final Path path = treeRootDir.resolve('docs/antora.yml')

if (project.version.endsWith('-SNAPSHOT')) {
    println 'not updating ' + path + ' on snapshot version'
} else {
    println 'Updating ' + path
    final String content = path.getText('UTF-8')
    final String versionReplacement = 'version: ' project.version
    println ' - seting ' + versionReplacement
    final Pattern versionPattern = ~'version: [^\\s]+'
    final String newContentString = versionPattern.matcher(content).replaceFirst(versionReplacement)

    if (!newContentString.equals(content)) {
        println 'Updated ' + path
        Files.write(path, newContentString.getBytes('UTF-8'))
    } else {
        println 'No change in ' + path
    }
}
