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

def makeTestClassNamesUnique(File sourceDir, String classNamePrefix) {
    if (!classNamePrefix.isEmpty()) {
        sourceDir.eachFileRecurse { file ->
            String originalName = file.name
            if ((originalName.endsWith("Test.java") || originalName.endsWith("IT.java")) && !originalName.startsWith(classNamePrefix)) {
                String className = originalName.replace(".java", "")
                String newClassName = "${classNamePrefix}${className}"

                String content = file.text
                content = content.replaceAll("${className}(?!EnvCustomizer)", "${classNamePrefix}${className}")

                if (originalName.endsWith("IT.java")) {
                    String originalExtendsClassName = className.replace("IT", "Test")
                    String extendsClassName = newClassName.replace("IT", "Test")
                    content = content.replaceAll(originalExtendsClassName, extendsClassName)
                }

                file.write(content)

                String path = file.absolutePath.replace(originalName, "${classNamePrefix}${originalName}")
                file.renameTo(path)
            }
        }
    }
}