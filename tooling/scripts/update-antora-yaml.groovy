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
 * Update the version in antora.yml
 */
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import java.util.regex.Pattern
import java.util.regex.Matcher

final String PROJECT_BRANCH_ROOT = ''

final Path treeRootDir = Paths.get(properties['maven.multiModuleProjectDirectory'])

final Path antoraYmlPath = treeRootDir.resolve('docs/antora.yml')
final Path sourceMapPath = treeRootDir.resolve('docs/source-map.yml')

println 'Examining ' + antoraYmlPath
final String antoraYmlContent = antoraYmlPath.getText('UTF-8')
final Pattern versionPattern = ~'version: ([^\\s]+)'
final Matcher versionMatcher = versionPattern.matcher(antoraYmlContent)
if (versionMatcher.find()) {
    final String originalVersion = versionMatcher.group(1)
    if (originalVersion.equals('next') && !project.version.endsWith('-SNAPSHOT')) {
        final String docVersion = project.version.substring(0, project.version.lastIndexOf('.') + 1) + 'x'
        final String versionReplacement = 'version: ' + docVersion

        String newContentString = versionMatcher.replaceFirst(versionReplacement)

        final Pattern removePattern = ~'(display-version: [^\\n]+\\n)|([ \\t]*prerelease: [^\\s]+\\n)'
        final Matcher removeMatcher = removePattern.matcher(newContentString)
        newContentString = removeMatcher.replaceAll('')

        println ' - setting ' + versionReplacement + ' in ' + antoraYmlPath

        Files.write(antoraYmlPath, newContentString.getBytes('UTF-8'))

        final String sourceMapContent = sourceMapPath.getText('UTF-8')
        final Pattern branchPattern = ~'([ \\t]*- branch: )([^\\s]+)'
        final Matcher branchMatcher = branchPattern.matcher(sourceMapContent)
        if (branchMatcher.find()) {
            final String originalBranch = branchMatcher.group(2)
            if ('main'.equals(originalBranch)) {
                final String newBranch = PROJECT_BRANCH_ROOT + docVersion
                final String branchReplacement = branchMatcher.group(1) + newBranch
                final String newSourceMapContent = branchMatcher.replaceFirst(branchReplacement)

                println ' - setting ' + branchReplacement + ' in ' + sourceMapPath

                Files.write(sourceMapPath, newSourceMapContent.getBytes('UTF-8'))
            }
        } else {
            println 'expected branch not found in ' + sourceMapPath + ': please examine!'
        }
   } else {
        println 'Version is already correct in ' + antoraYmlPath
    }
} else {
    println 'No version found in ' + antoraYmlPath
}
