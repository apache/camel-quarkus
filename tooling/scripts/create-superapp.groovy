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

import groovy.xml.*

def rootPom = new XmlParser().parse(new File('pom.xml'))
def camelQuarkusVersion = rootPom.version.text()
def quarkusVersion = rootPom.properties.'quarkus.version'?.text()
def mavenEnforcerPluginVersion = rootPom.properties.'maven-enforcer-plugin.version'?.text()

// Get org.apache.camel.quarkus dependencies from camel-quarkus-bom
def cqBomPath = "${System.properties['user.home']}/.m2/repository/org/apache/camel/quarkus/camel-quarkus-bom/${camelQuarkusVersion}/camel-quarkus-bom-${camelQuarkusVersion}.pom"
def bom = new XmlParser().parse(new File(cqBomPath))
def cqBomDependencies = bom.dependencyManagement.dependencies?.dependency?.findAll {
    it.groupId.text().startsWith('org.apache.camel.quarkus') && !it.artifactId.text().contains('-support')
}

// Create a 'super' POM with all camel-quarkus-* dependencies
def writer = new StringWriter()
def xml = new MarkupBuilder(writer)

xml.project(xmlns: "http://maven.apache.org/POM/4.0.0",
        "xmlns:xsi": "http://www.w3.org/2001/XMLSchema-instance",
        "xsi:schemaLocation": "http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd") {
    modelVersion('4.0.0')
    groupId('org.apache.camel.quarkus')
    artifactId('camel-quarkus-superapp')
    version(camelQuarkusVersion)
    packaging('pom')
    build {
        plugins {
            plugin {
                groupId('org.apache.maven.plugins')
                artifactId('maven-enforcer-plugin')
                version(mavenEnforcerPluginVersion)
                executions {
                    execution {
                        goals {
                            goal('enforce')
                        }
                        configuration {
                            rules {
                                dependencyConvergence()
                            }
                        }
                    }
                }
            }
        }
    }
    dependencyManagement {
        dependencies {
            dependency{
                groupId('io.quarkus')
                artifactId('quarkus-bom')
                version(quarkusVersion)
                type('pom')
                scope('import')
            }
            dependency{
                groupId('org.apache.camel.quarkus')
                artifactId('camel-quarkus-bom')
                version(camelQuarkusVersion)
                type('pom')
                scope('import')
            }
        }
    }
    dependencies {
        cqBomDependencies.each { dep ->
            dependency {
                groupId(dep.groupId.text())
                artifactId(dep.artifactId.text())
            }
        }
    }
}

def tmp = System.getenv('RUNNER_TEMP')
def superAppDir = new File("${tmp}/camel-quarkus-superapp")
superAppDir.mkdirs()

new File("${superAppDir}/pom.xml").text = writer.toString()
