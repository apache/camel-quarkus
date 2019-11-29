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

import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import java.util.regex.Matcher
import java.util.regex.Pattern

@groovy.transform.Field
final Path baseDir = basedir.toPath()

final String[] sortDepManagementPaths = properties['sortDependencyManagementPaths'].split(',')
sortDependencyManagement(sortDepManagementPaths)

final String[] sortModulesPaths = properties['sortModulesPaths'].split(',')
sortModules(sortModulesPaths)

void sortDependencyManagement(String[] pomPaths) {
    for (pomPath in pomPaths) {
        final Path pomXmlPath = baseDir.resolve(pomPath.trim())
        final String xmlSource = new String(Files.readAllBytes(pomXmlPath), 'UTF-8')

        final Pattern sortSpanPattern = Pattern.compile('(a\\.\\.z[^>]*>)(.*)</dependencies>(\\r?\\n)([ ]*)</dependencyManagement>', Pattern.DOTALL)
        final Pattern groupIdPattern = Pattern.compile('<groupId>([^<]+)</groupId>')

        final Matcher matcher = sortSpanPattern.matcher(xmlSource)
        if (matcher.find()) {
            String dependenciesString = matcher.group(2)
            final String eol = matcher.group(3)
            final String indent = matcher.group(4)

            dependenciesString = dependenciesString.replaceAll('<!--\\$[^>]*\\$-->', '')
            final String[] dependenciesArray = dependenciesString.split('</dependency>')
            /* Sort by adding to a TreeMap */
            final Map<String, Map<String, String>> sortedDeps = new TreeMap<>();
            for (dep in dependenciesArray) {
                dep = dep.trim()
                if (!dep.isEmpty()) {
                    String key = dep
                            .replaceAll('>[ \n\r\t]+', '>')
                            .replaceAll('[ \n\r\t]+<', '<')
                    final Matcher gMatcher = groupIdPattern.matcher(key)
                    gMatcher.find()
                    final String groupId = gMatcher.group(1)
                    key = key.replaceAll('<[^>]+>', ' ').replaceAll(' +', ' ')

                    Map<String, String> groupMap = sortedDeps.get(groupId)
                    if (groupMap == null) {
                        groupMap = new TreeMap<String, String>()
                        sortedDeps.put(groupId, groupMap)
                    }
                    groupMap.put(key, dep);
                }
            }
            final StringBuilder result = new StringBuilder(xmlSource)
            result.setLength(matcher.end(1))

            final Appender appender = new Appender(eol, indent, sortedDeps, result)

            appender.appendGroup('org.apache.camel', true)
            appender.appendGroup('org.apache.camel.quarkus', true)

            appender.appendOther()
            appender.result().append(eol).append(indent).append(indent).append(xmlSource.substring(matcher.end(2)))

            Files.write(pomXmlPath, result.toString().getBytes('UTF-8'));
        } else {
            throw new RuntimeException('Could not match ' + sortSpanPattern + ' in ' + pomXmlPath)
        }
    }
}

class Appender {
    private final Set<String> processedGroupIds = new HashSet<>()
    private final String eol
    private final String indent
    private final Map<String, Map<String, String>> sortedDeps
    private final StringBuilder result

    public Appender(String eol, String indent, Map<String, Map<String, String>> sortedDeps, StringBuilder result) {
        this.eol = eol
        this.indent = indent
        this.sortedDeps = sortedDeps
        this.result = result
    }

    public void comment(String comment) {
        result.append(eol).append(eol)
                .append(indent).append(indent).append(indent).append('<!--$ '+ comment +' $-->')
    }

    public void appendGroup(String groupId, boolean isComment) {
        final Map<String, String> deps = sortedDeps.get(groupId)
        if (deps == null || processedGroupIds.contains(groupId)) {
            return
        }
        processedGroupIds.add(groupId)
        if (isComment) {
            comment(groupId)
        }
        for (dep in deps.values()) {
            result.append(eol)
                    .append(indent).append(indent).append(indent).append(dep)
                    .append(eol).append(indent).append(indent).append(indent).append('</dependency>')
        }
    }

    public void appendOther() {
        if (processedGroupIds.size() < sortedDeps.size()) {
            comment('Other third party dependencies')
            for (group in sortedDeps.entrySet()) {
                appendGroup(group.getKey(), false)
            }
        }
    }

    public StringBuilder result() {
        return result
    }
}

void sortModules(String[] sortModulesPaths) {
    for (pomPath in sortModulesPaths) {
        final Path pomXmlPath = basedir.toPath().resolve(pomPath.trim())
        final String xmlSource = new String(Files.readAllBytes(pomXmlPath), 'UTF-8')

        final Pattern sortSpanPattern = Pattern.compile('(a\\.\\.z[^>]*>)(.*)(\\r?\\n)([ ]*)</modules>', Pattern.DOTALL)

        final Matcher matcher = sortSpanPattern.matcher(xmlSource)
        if (matcher.find()) {
            final String modulesString = matcher.group(2)
            final String eol = matcher.group(3)
            final String indent = matcher.group(4)
            final String[] modulesArray = modulesString.split('[\r\n]+ *')
            final Map<String, String> sortedModules = new TreeMap<String, String>()
            for (module in modulesArray) {
                module = module.trim()
                if (!module.isEmpty()) {
                    String key = module
                            .replaceAll('>[ \n\r\t]+', '>')
                            .replaceAll('[ \n\r\t]+<', '<')
                    key = key.replaceAll('<[^>]+>', '')
                    if (!key.isEmpty()) {
                        sortedModules.put(key, module);
                    }
                }
            }

            final StringBuilder result = new StringBuilder(xmlSource)
            result.setLength(matcher.end(1))
            for (module in sortedModules.values()) {
                result.append(eol).append(indent).append(indent).append(module)
            }
            result.append(eol).append(indent).append(xmlSource.substring(matcher.end(4)))

            Files.write(pomXmlPath, result.toString().getBytes('UTF-8'));
        } else {
            throw new RuntimeException('Could not match ' + sortSpanPattern + ' in ' + pomXmlPath)
        }
    }
}
