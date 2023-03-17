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
import groovy.ant.AntBuilder

/* Copies source files from one source test modules into one destination module (`copy-tests.dest.module.dir`)
 * so that the tests can be executed. Use of ('copy-tests.exclude') allows to exclude files.
 */

final Path sourceDir = Paths.get(properties['copy-tests.source.dir'])
final Path destinationModuleDir = Paths.get(properties['copy-tests.dest.module.dir'])
final String excl = properties['copy-tests.excludes']
final String classNamePrefix = properties['group-tests.class.name.prefix'] ?: ""

copyResources(sourceDir.resolve('src/main/resources'), destinationModuleDir.resolve('target/classes'), excl)
copyResources(sourceDir.resolve('src/main/java'), destinationModuleDir.resolve('target/src/main/java'), excl)
copyResources(sourceDir.resolve('src/test/java'), destinationModuleDir.resolve('target/src/test/java'), excl)
copyResources(sourceDir.resolve('src/test/resources'), destinationModuleDir.resolve('target/test-classes'), excl)

String scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parent
File sourceFile = new File("${scriptDir}/group-test-utils.groovy")
Class groovyClass = new GroovyClassLoader(getClass().getClassLoader()).parseClass(sourceFile);
GroovyObject utils = (GroovyObject) groovyClass.getDeclaredConstructor().newInstance();
utils.makeTestClassNamesUnique(destinationModuleDir.resolve('target/src/test/java').toFile(), classNamePrefix)

static void copyResources(Path source, Path dest, String excl) {
    if (Files.exists(source)) {
        new AntBuilder().copy(todir: dest) {
             fileset(dir: source, includes: "**", excludes: excl)
        }
    }
}

