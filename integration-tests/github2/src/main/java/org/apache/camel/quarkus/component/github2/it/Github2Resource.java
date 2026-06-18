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
package org.apache.camel.quarkus.component.github2.it;

import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

@Path("/github2")
public class Github2Resource {

    // Upstream GetCommitFileProducer uses the same SHA for both getCommit() and getBlob() calls
    private static final String COMMIT_SHA = "6195efafd0a8100795247e35942b5c61fea79267";
    private static final String GITHUB_AUTH_PARAMS = "oauthToken={{env:GITHUB_TOKEN:}}";

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/get")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getCamelQuarkusReadme() throws Exception {
        Config config = ConfigProvider.getConfig();
        Optional<String> wireMockUrl = config.getOptionalValue("wiremock.url", String.class);

        StringBuilder uri = new StringBuilder("github2:GETCOMMITFILE?repoOwner=apache&repoName=camel-quarkus&");
        uri.append(GITHUB_AUTH_PARAMS);
        wireMockUrl.ifPresent(url -> uri.append("&apiUrl=").append(url));

        return producerTemplate.requestBody(
                uri.toString(),
                COMMIT_SHA,
                String.class);
    }

    @Path("/repository")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getRepositoryFullName() throws Exception {
        GHRepository repo = createGitHubClient().getRepository("apache/camel-quarkus");
        return repo.getFullName();
    }

    @Path("/commit/author")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getCommitAuthor() throws Exception {
        GHRepository repo = createGitHubClient().getRepository("apache/camel-quarkus");
        GHCommit commit = repo.getCommit(COMMIT_SHA);
        return commit.getCommitShortInfo().getAuthor().getName();
    }

    private GitHub createGitHubClient() throws Exception {
        Config config = ConfigProvider.getConfig();
        Optional<String> wireMockUrl = config.getOptionalValue("wiremock.url", String.class);
        String token = System.getenv("GITHUB_TOKEN");

        GitHubBuilder builder = new GitHubBuilder()
                .withOAuthToken(token != null ? token : "");
        wireMockUrl.ifPresent(url -> builder.withEndpoint(url));
        return builder.build();
    }
}
