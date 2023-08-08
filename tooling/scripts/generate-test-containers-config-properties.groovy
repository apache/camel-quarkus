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
 * Writes all Maven properties suffixed with 'container.image' to target/test-classes/META-INF/microprofile-config.properties
 */
def fileContent = project.properties.findAll { key, value ->
    key.endsWith('container.image')
}.collect { key, value ->
    "${key}=${value}"
}.join('\n')

File testClasses = new File("${project.build.testOutputDirectory}")
if (testClasses.exists()) {
    File metaInfDir = new File("${testClasses.absolutePath}/META-INF")
    metaInfDir.mkdir()

    File file = new File("${metaInfDir.absolutePath}/microprofile-config.properties")
    file.write(fileContent, 'UTF-8')
}
