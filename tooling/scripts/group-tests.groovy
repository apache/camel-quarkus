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
 * Group tests in a directory to a single Maven module
 */
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import java.util.stream.Stream
import java.util.stream.Collectors
import java.util.regex.Pattern
import java.util.regex.Matcher


final Path sourceDir = Paths.get(properties['group-tests.source.dir'])
final String[] concatRelPaths = properties['group-tests.concat.rel.paths'].split('[\\s,]+')
final Path destinationModuleDir = Paths.get(properties['group-tests.dest.module.dir'])

final Map<String, StringBuilder> mergedFiles = new HashMap<>()
concatRelPaths.each {relPath -> mergedFiles.put(relPath, new StringBuilder())}

Files.list(sourceDir)
    .filter {p -> Files.exists(p.resolve('pom.xml'))}
    .sorted()
    .forEach {p ->
        mergedFiles.each { relPath, sb -> sb.append(p.resolve(relPath).getText('UTF-8') + '\n') }
    }

mergedFiles.each { relPath, sb ->
    String destRelPath = relPath.replace('src/main/resources/', 'target/classes/').replace('src/test/resources/', 'target/test-classes/')
    Path destPath = destinationModuleDir.resolve(destRelPath)
    Files.createDirectories(destPath.getParent())
    Files.write(destPath, sb.toString().getBytes('UTF-8'))
}

