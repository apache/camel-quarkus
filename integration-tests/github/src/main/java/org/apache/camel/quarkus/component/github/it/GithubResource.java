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
package org.apache.camel.quarkus.component.github.it;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.github.GitHubConstants;
import org.apache.camel.impl.event.CamelContextStartedEvent;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.DataService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

@Path("/github")
public class GithubResource {

    private static final String GITHUB_AUTH_PARAMS = "oauthToken={{env:GITHUB_TOKEN:}}";

    @Inject
    ProducerTemplate producerTemplate;

    public void onContextStart(@Observes CamelContextStartedEvent event) {
        Config config = ConfigProvider.getConfig();
        Optional<String> wireMockUrl = config.getOptionalValue("wiremock.url", String.class);
        if (wireMockUrl.isPresent()) {
            // Force the GH client to use the WireMock proxy
            try {
                CamelContext context = event.getContext();
                URI wireMockUri = new URI(wireMockUrl.get());
                GitHubClient client = new GitHubClient(wireMockUri.getHost(), wireMockUri.getPort(), wireMockUri.getScheme()) {
                    @Override
                    protected String configureUri(String uri) {
                        // Prevent the original impl adding an unwanted /v3 path prefix
                        return uri;
                    }
                };
                DataService dataService = new DataService(client);
                RepositoryService repositoryService = new RepositoryService(client);
                context.getRegistry().bind(GitHubConstants.GITHUB_DATA_SERVICE, dataService);
                context.getRegistry().bind(GitHubConstants.GITHUB_REPOSITORY_SERVICE, repositoryService);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Path("/get")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getCamelQuarkusReadme() throws Exception {
        CommitFile commitFile = new CommitFile();
        commitFile.setSha("6195efafd0a8100795247e35942b5c61fea79267");

        return producerTemplate.requestBody(
                "github:GETCOMMITFILE?repoOwner=apache&repoName=camel-quarkus&" + GITHUB_AUTH_PARAMS,
                commitFile, String.class);
    }
}
