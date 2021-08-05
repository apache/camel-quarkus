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

/* Copies source files from multiple source test modules (subdirs of `group-tests.source.dir`) into
 * one destination module (`group-tests.dest.module.dir`) so that the tests can be executed all at once
 */

final Path sourceDir = Paths.get(properties['group-tests.source.dir'])
final String[] concatRelPaths = properties['group-tests.concat.rel.paths'].split('[\\s,]+')
final Path destinationModuleDir = Paths.get(properties['group-tests.dest.module.dir'])
/* Property names whose values originating from distinct application.properties files can be concatenated using comma as a separator */
final Set<String> commaConcatenatePropertyNames = ["quarkus.native.resources.includes", "quarkus.native.resources.excludes"] as Set;

final Map<String, ResourceConcatenator> mergedFiles = new HashMap<>()
concatRelPaths.each {relPath -> mergedFiles.put(relPath, new ResourceConcatenator(commaConcatenatePropertyNames)) }

Files.list(sourceDir)
    .filter { p -> Files.exists(p.resolve('pom.xml')) }
    .sorted()
    .forEach { p ->
        mergedFiles.each { relPath, cat ->
            cat.append(p.resolve(relPath))
        }
        copyResources(p.resolve('src/main/java'), destinationModuleDir.resolve('target/src/main/java'))
        copyResources(p.resolve('src/test/java'), destinationModuleDir.resolve('target/src/test/java'))
        copyResources(p.resolve('src/main/resources'), destinationModuleDir.resolve('target/classes'))
        copyResources(p.resolve('src/test/resources'), destinationModuleDir.resolve('target/test-classes'))
    }

mergedFiles.each { relPath, cat ->
    String destRelPath = relPath.replace('src/main/resources/', 'target/classes/').replace('src/test/resources/', 'target/test-classes/')
    Path destPath = destinationModuleDir.resolve(destRelPath)
    cat.store(destPath)
}

static void copyResources(Path source, Path dest) {
    if (Files.exists(source)) {
        new AntBuilder().copy(todir: dest) {
            fileset(dir: source, includes: "**")
        }
    }
}

class ResourceConcatenator {
    private final StringBuilder sb = new StringBuilder()
    private final Properties props = new Properties()
    private final List<Path> visitedPaths = new ArrayList<>()
    private final Set<String> commaConcatenatePropertyNames;

    public ResourceConcatenator(Set<String> commaConcatenatePropertyNames) {
        this.commaConcatenatePropertyNames = commaConcatenatePropertyNames;
    }
    public ResourceConcatenator append(Path path) {
        if (Files.exists(path)) {
            if (path.getFileName().toString().endsWith(".properties")) {
                Properties newProps = new Properties()
                path.withInputStream { is ->
                    newProps.load(is)
                }
                newProps.each { key, val ->
                    if (props.containsKey(key) && !props.get(key).equals(val)) {
                        if (commaConcatenatePropertyNames.contains(key)) {
                            props.put(key, props.get(key) + "," + val);
                        } else {
                            throw new IllegalStateException("Conflicting property value "+ key +" = "+ val +": found in "+ path + " conflicting with some of " + visitedPaths);
                        }
                    } else {
                        props.put(key, val);
                    }
                }
            } else {
                sb.append(path.getText('UTF-8') + '\n')
            }
            visitedPaths.add(path)
        }
        return this
    }

    public void store(Path path) {
        Files.createDirectories(path.getParent())
        if (path.getFileName().toString().endsWith(".properties")) {
            path.withOutputStream { out ->
                props.store(out, "")
            }
        } else {
            Files.write(path, sb.toString().getBytes('UTF-8'))
        }
    }
}
