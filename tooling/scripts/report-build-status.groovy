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
 * A script to report on the build status of synchronization for branches camel-main and quarkus-main.
 *
 * A GitHub issue ID is passed to this script from the GitHub workflow. The issue is inteded to be repeatedly closed / reopened
 * whenever a build workflow run is successful / unsuccessful.
 *
 * If failures were encountered in the build, a comment is appended to a specified GitHub issue, with the body containing
 * information about the commit SHA and a link to the build.
 *
 * If the build was successful, the open GitHub issue relating to the branch build will be closed.
 *
 * The script also outputs a GitHub action step variable named 'overall_build_status', this is used by the build to determine whether it
 * should automatically merge the latest changes from the main branch, to the target branch.
 */

final String TOKEN = properties['token']
final String STATUS = properties['status'].toLowerCase(Locale.US)
final String BUILD_ID = properties['buildId']
final String REPO = properties['repo']
final String BRANCH = properties['branch']
final String BRANCH_NAME = "${BRANCH.split('-')[0].capitalize()} ${BRANCH.split('-')[1].capitalize()}"
final String BRANCH_COMMIT = properties['branch-commit'] ?: 'Unknown'
final String ACTIONS_URL = "https://github.com/${REPO}/actions/runs/${BUILD_ID.split("-")[0]}"
final String BRANCH_URL = "https://github.com/${REPO}/tree/${BRANCH}"
final String ISSUE_LABEL = "build/${BRANCH}"
final Integer ISSUE_ID = properties['issueId'] as Integer

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

GHIssue issue = repository.getIssue(ISSUE_ID)

if (issue == null) {
    println("Unable to find the issue with id ${ISSUE_ID} in project ${REPO}")
    System.exit(1)
} else {
    println("Report issue found: ${issue.getTitle()} - ${issue.getHtmlUrl()}")
}

if (STATUS == "failure") {
    if (Utils.workflowHasPreviousFailures(issue, BUILD_ID)) {
        println("Workflow for Build ID ${BUILD_ID} has already reported failures - exiting")
    } else {
        if (issue.getState() == GHIssueState.CLOSED) {
            issue.reopen()
        }

        issue.comment("""The [${BRANCH}](${BRANCH_URL}) branch build has failed:

* Build ID: ${BUILD_ID}
* Camel Quarkus Commit: ${camelQuarkusCommit}
* ${BRANCH_NAME} Commit: ${BRANCH_COMMIT}
* Link to build: ${ACTIONS_URL}""")

        println("Commented on issue ${issue.getHtmlUrl()}")
    }
}

if (STATUS == "verify") {
    if (Utils.workflowHasPreviousFailures(issue, BUILD_ID)) {
        println("Overall build status is: failure")
        new File(System.getenv("GITHUB_OUTPUT")).append("overall_build_status=failure")
        return
    } else {
        if (issue.getState() == GHIssueState.OPEN) {
            final GHIssueComment comment = issue.comment("""Build fixed with:

* Camel Quarkus Commit: ${camelQuarkusCommit}
* ${BRANCH_NAME} Commit: ${BRANCH_COMMIT}
* Link to build: ${ACTIONS_URL}""")

            issue.close()
            println("Comment added on issue ${issue.getHtmlUrl()} - ${comment.getHtmlUrl()}, the issue has also been closed")
        }
    }

    new File(System.getenv("GITHUB_OUTPUT")).append("overall_build_status=success")
}
