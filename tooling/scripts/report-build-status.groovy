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

import org.kohsuke.github.*

/**
 * A script to report on the build status of synchronization for branches camel-master and quarkus-master.
 *
 * If failures were encountered in the build, a new GitHub issue is opened labeled with build/quarkus-master or build/camel-master, with the body containing
 * information about the commit SHA and a link to the build. If an existing open issue with the appropriate label exists, then
 * a new comment about the build failure is added.
 *
 * If the build was successful, any open GitHub issue relating to the branch build will be closed.
 *
 * The script also outputs a GitHub action step variable named 'overall_build_status', this is used by the build to determine whether it
 * should automatically merge the latest changes from the master branch, to the target branch.
 */

final String TOKEN = properties['token']
final String STATUS = properties['status'].toLowerCase(Locale.US)
final String BUILD_ID = properties['buildId']
final String REPO = properties['repo']
final String BRANCH = properties['branch']
final String BRANCH_NAME = "${BRANCH.split('-')[0].capitalize()} ${BRANCH.split('-')[1].capitalize()}"
final String ACTIONS_URL = "https://github.com/${REPO}/actions/runs/${BUILD_ID.split("-")[0]}"
final String BRANCH_URL = "https://github.com/${REPO}/tree/${BRANCH}"
final String ISSUE_LABEL = "build/${BRANCH}"

class Utils {
    static boolean workflowHasPreviousFailures(GHIssue issue, String buildId) {
        String issueCommentMatch = "Build ID: ${buildId}"
        int failureCount = issue.getComments().count { comment ->
            comment.getBody().contains(issueCommentMatch)
        }
        return issue.getBody().contains(issueCommentMatch) || failureCount > 0
    }
}

if (STATUS == "cancelled") {
    println("Job status is cancelled - exiting")
    return
}
println("Workflow status is ${STATUS}")

final GitHub github = new GitHubBuilder().withOAuthToken(TOKEN, "github-actions").build()
final GHRepository repository = github.getRepository(REPO)
final String camelQuarkusCommit = "git rev-parse HEAD".execute().text

GHIssue issue = null
def issues = repository.getIssues(GHIssueState.OPEN)
issues.each { i ->
    i.getLabels().each { label ->
        if (label.getName() == ISSUE_LABEL) {
            issue = i
            return
        }
    }
}

if (issue == null) {
    println("Unable to find the issue labeled ${ISSUE_LABEL} in project ${REPO}")
} else {
    println("Report issue found: ${issue.getTitle()} - ${issue.getHtmlUrl()}")
}

if (STATUS == "failure") {
    if (issue == null) {
        final String issueBody = """The [${BRANCH}](${BRANCH_URL}) branch build is failing:

* Build ID: ${BUILD_ID}
* Commit: ${camelQuarkusCommit}
* Link to build: ${ACTIONS_URL}
"""

        issue = repository.createIssue("[CI] - ${BRANCH_NAME} Branch Build Failure")
                .body(issueBody)
                .label(ISSUE_LABEL)
                .create();

        println("Created new issue ${issue.getHtmlUrl()}")
    } else {
        if (Utils.workflowHasPreviousFailures(issue, BUILD_ID)) {
            println("Workflow for Build ID ${BUILD_ID} has already reported failures - exiting")
        } else {
            issue.comment("""The [${BRANCH}](${BRANCH_URL}) branch build is still failing:

* Build ID: ${BUILD_ID}
* Commit: ${camelQuarkusCommit}
* Link to build: ${ACTIONS_URL}""")

            println("Commented on issue ${issue.getHtmlUrl()}")
        }
    }
}

if (STATUS == "verify") {
    if (issue != null) {
        if (Utils.workflowHasPreviousFailures(issue, BUILD_ID)) {
            println("Overall build status is: failure")
            println "::set-output name=overall_build_status::failure"
            return
        } else {
            final GHIssueComment comment = issue.comment("""Build fixed with:

* Commit: ${camelQuarkusCommit}
* Link to build: ${ACTIONS_URL}""")
            issue.close()
            println("Comment added on issue ${issue.getHtmlUrl()} - ${comment.getHtmlUrl()}, the issue has also been closed")
        }
    }
    println "::set-output name=overall_build_status::success"
}
