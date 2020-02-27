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
 * A script for sorting child modules and dependencyManagement dependencies in pom.xml files.
 * Only elements will be sorted that occur after a comment containing the {@code a..z} marker string.
 */

import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import java.util.regex.Matcher
import java.util.regex.Pattern

import groovy.util.NodeList

@groovy.transform.Field
final Path baseDir = basedir.toPath()

final String[] sortDepManagementPaths = properties['sortDependencyManagementPaths'].split(',')
sortDependencyManagement(sortDepManagementPaths)

final String[] sortModulesPaths = properties['sortModulesPaths'].split(',')
sortModules(sortModulesPaths)

final String[] updateMvndRuleDirs = properties['updateMvndRuleDirs'].split(',')
updateMvndRules(updateMvndRuleDirs)

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

void updateMvndRules(String[] updateMvndRuleDirs) {
    final Set<String> extensionArtifactIds = [] as TreeSet
    Files.list(baseDir.resolve('extensions'))
            .filter { p -> Files.isDirectory(p) && Files.exists(p.resolve('pom.xml')) && Files.exists(p.resolve('runtime')) }
            .map { p -> p.getFileName().toString() }
            .filter { dirName -> !dirName.equals('support') }
            .map { dirName -> 'camel-quarkus-' + dirName }
            .forEach { aid -> extensionArtifactIds << aid }

    Files.list(baseDir.resolve('extensions/support'))
            .filter { p -> Files.isDirectory(p) && Files.exists(p.resolve('pom.xml')) && Files.exists(p.resolve('runtime')) }
            .map { p -> p.getFileName().toString() }
            .map { dirName -> 'camel-quarkus-support-' + dirName }
            .forEach { aid -> extensionArtifactIds << aid }

    Files.list(baseDir.resolve('integration-tests/support'))
            .filter { p -> Files.isDirectory(p) && Files.exists(p.resolve('pom.xml')) && Files.exists(p.resolve('runtime')) }
            .map { p -> p.getFileName().toString() }
            .map { dirName -> 'camel-quarkus-integration-test-support-' + dirName + '-ext' }
            .forEach { aid -> extensionArtifactIds << aid }

    /* Policy may disappear at some point */
    final boolean policyExtensionExists = extensionArtifactIds.contains('camel-quarkus-support-policy')

    final Pattern dependenciesPattern = Pattern.compile('([^\n<]*)<dependenc')
    final Pattern propsPattern = Pattern.compile('([^\n<]*)</properties>')
    final Pattern rulePattern = Pattern.compile('<mvnd.builder.rule>[^<]*</mvnd.builder.rule>')

    for (updateMvndRuleDir in updateMvndRuleDirs) {
        Files.list(baseDir.resolve(updateMvndRuleDir))
                .filter { p -> Files.isDirectory(p) && !'support'.equals(p.getFileName().toString()) }
                .map { p -> p.resolve('pom.xml') }
                .filter { p -> Files.exists(p) }
                .forEach { pomXmlPath ->

                        final Path relativePomPath = baseDir.relativize(pomXmlPath)

                        String pomXmlText = pomXmlPath.toFile().getText('UTF-8')

                        Node pomXmlProject = null
                        try {
                            pomXmlProject = new XmlParser().parseText(pomXmlText)
                        } catch (Exception e) {
                            throw new RuntimeException('Could not parse ' + relativePomPath, e)
                        }
                        final List<String> extensionDependencies = pomXmlProject.dependencies.dependency
                                .findAll { dep -> "org.apache.camel.quarkus".equals(dep.groupId.text()) && extensionArtifactIds.contains(dep.artifactId.text()) }
                                .collect { dep -> dep.artifactId.text() + '-deployment' }
                        if (policyExtensionExists) {
                            extensionDependencies.add('camel-quarkus-support-policy-deployment')
                        }

                        final String expectedRule = extensionDependencies
                                .toSorted()
                                .join(',')

                        final Matcher depsMatcher = dependenciesPattern.matcher(pomXmlText)
                        if (depsMatcher.find()) {
                            final String indent = depsMatcher.group(1)
                            final int insertionPos = depsMatcher.start()

                            final NodeList props = pomXmlProject.properties
                            if (props.isEmpty()) {
                                final String insert = indent + '<properties>\n' +
                                    indent + indent + '<!-- mvnd, a.k.a. Maven Daemon: https://github.com/gnodet/mvnd -->\n' +
                                    indent + indent + '<!-- The following rule tells mvnd to build the listed deployment modules before this module. -->\n' +
                                    indent + indent + '<!-- This is important because mvnd builds modules in parallel by default. The deployment modules are not -->\n' +
                                    indent + indent + '<!-- explicit dependencies of this module in the Maven sense, although they are required by the Quarkus Maven plugin. -->\n' +
                                    indent + indent + '<!-- Please update rule whenever you change the dependencies of this module by running -->\n' +
                                    indent + indent + '<!--     mvn process-resources -Pformat    from the root directory -->\n' +
                                    indent + indent + '<mvnd.builder.rule>' + expectedRule + '</mvnd.builder.rule>\n' +
                                    indent + '</properties>\n\n'
                                pomXmlText = new StringBuilder(pomXmlText).insert(insertionPos, insert).toString()
                                Files.write(pomXmlPath, pomXmlText.getBytes('UTF-8'))
                            } else {
                                final NodeList mvndRule = props.'mvnd.builder.rule'
                                if (mvndRule.isEmpty()) {
                                    final Matcher propsMatcher = propsPattern.matcher(pomXmlText)
                                    if (propsMatcher.find()) {
                                        final int insPos = propsMatcher.start()
                                        final String insert = indent + indent + '<mvnd.builder.rule>' + expectedRule + '</mvnd.builder.rule>\n'
                                        pomXmlText = new StringBuilder(pomXmlText).insert(insPos, insert).toString()
                                        Files.write(pomXmlPath, pomXmlText.getBytes('UTF-8'))
                                    } else {
                                        throw new IllegalStateException('Could not find ' + propsPattern.pattern() + ' in ' + relativePomPath)
                                    }
                                } else {
                                    final String actualRule = mvndRule.get(0).text()
                                            .split(',')
                                            .collect{ it -> it.trim() }
                                            .toSorted()
                                            .join(',')
                                    if (!expectedRule.equals(actualRule)) {
                                        final Matcher ruleMatcher = rulePattern.matcher(pomXmlText)
                                        if (ruleMatcher.find()) {
                                            final StringBuffer buf = new StringBuffer(pomXmlText.length() + 128)
                                            final String replacement = '<mvnd.builder.rule>' + expectedRule + '</mvnd.builder.rule>'
                                            ruleMatcher.appendReplacement(buf, Matcher.quoteReplacement(replacement))
                                            ruleMatcher.appendTail(buf)
                                            Files.write(pomXmlPath, buf.toString().getBytes('UTF-8'))
                                        } else {
                                            throw new IllegalStateException('Could not find ' + rulePattern.pattern() + ' in ' + relativePomPath)
                                        }
                                    }
                                }
                            }

                        } else {
                            throw new IllegalStateException('Could not find ' + dependenciesPattern.pattern() + ' in ' + relativePomPath)
                        }



                }
    }
}
