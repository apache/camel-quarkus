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
import org.yaml.snakeyaml.Yaml
import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.codehaus.plexus.util.xml.pull.XmlPullParserException

final Path treeRootDir = project.getBasedir().toPath()

final String testCategoriesDefRelPath = 'tooling/scripts/test-categories.yaml'
final Path jobDefPath = treeRootDir.resolve(testCategoriesDefRelPath)
final Set<String> executedBaseNames = [] as Set

// Add any ignored itest modules here. Or prefix the module name with '#' to disable it
final List<String> excludedModules = [
    'infinispan-common',
    'messaging',
    'nats'
] as List

final Yaml parser = new Yaml()
def testCategoryConfig = parser.load((jobDefPath.toFile()).text)

Set<String> itestModules
try (Reader r = Files.newBufferedReader(treeRootDir.resolve('integration-tests/pom.xml'), java.nio.charset.StandardCharsets.UTF_8)) {
    itestModules = new HashSet<>(new MavenXpp3Reader().read(r).getModules());
} catch (XmlPullParserException | IOException e) {
    throw new RuntimeException("Could not parse " + path, e);
}


def modules = []
testCategoryConfig.each { k, v ->
    v.each {
        modules << it
    }
}
modules.each { executedBaseNames.addAll(it.trim().split(' ')) }

final Set<String> missingBaseNames = [] as TreeSet
final Set<String> itestBaseNames = itestModules.stream()
        .filter{ dirName -> !excludedModules.contains(dirName) }
        .filter{ dirName -> !executedBaseNames.contains('#' + dirName) }
        .filter{ dirName -> !executedBaseNames.contains(dirName) }
        .forEach{ dirName -> missingBaseNames.add(dirName) }

if (!missingBaseNames.isEmpty()) {
    throw new IllegalStateException('Integration tests not executed by the CI:\n\n    ' +
        missingBaseNames.join('\n    ') +
        '\n\n Add the missing test module(s) to ' + testCategoriesDefRelPath)
}
