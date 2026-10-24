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
package org.apache.camel.quarkus.component.dropbox.it;

import java.util.Map;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import org.apache.camel.quarkus.test.wiremock.WireMockTestResourceLifecycleManager;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;

/**
 * Dropbox splits its API across api.dropboxapi.com and content.dropboxapi.com, so stubs are defined here
 * rather than recorded against a single target. The endpoint paths do not overlap, so one server serves both.
 */
public class DropboxTestResource extends WireMockTestResourceLifecycleManager {

    private static final String[] DROPBOX_ENV_VARS = {
            "DROPBOX_ACCESS_TOKEN",
            "DROPBOX_ACCESS_TOKEN_EXPIRES_IN",
            "DROPBOX_API_KEY",
            "DROPBOX_API_SECRET",
            "DROPBOX_CLIENT_IDENTIFIER",
            "DROPBOX_REFRESH_TOKEN",
    };
    private static final String SCENARIO = "dropbox-file";
    private static final String FILE_DELETED = "file-deleted";
    private static final String FILE_PATH = DropboxResource.REMOTE_PATH + DropboxResource.FILE_NAME;
    private static final String FILE_METADATA = """
            {
              ".tag": "file",
              "name": "%s",
              "id": "id:camel-quarkus-test",
              "client_modified": "2026-01-01T00:00:00Z",
              "server_modified": "2026-01-01T00:00:00Z",
              "rev": "0123456789abcdef",
              "size": %d,
              "path_lower": "%s",
              "path_display": "%s",
              "is_downloadable": true
            }
            """.formatted(DropboxResource.FILE_NAME, DropboxResource.FILE_CONTENT.length(), FILE_PATH, FILE_PATH);

    @Override
    public Map<String, String> start() {
        Map<String, String> properties = super.start();
        if (server != null) {
            stubDropboxApi();
            for (String envVar : DROPBOX_ENV_VARS) {
                properties.put(envVar, "test-" + envVar.toLowerCase());
            }
            properties.put("DROPBOX_ACCESS_TOKEN_EXPIRES_IN", String.valueOf(Long.MAX_VALUE));
        }
        return properties;
    }

    private void stubDropboxApi() {
        // The producer checks whether the remote path exists before uploading
        server.stubFor(post(urlPathEqualTo("/2/files/get_metadata"))
                .willReturn(pathError("not_found")));

        server.stubFor(post(urlPathEqualTo("/2/files/upload"))
                .willReturn(json(FILE_METADATA)));

        // The consumer lists the remote path as a folder before falling back to downloading it as a file
        server.stubFor(post(urlPathEqualTo("/2/files/list_folder"))
                .inScenario(SCENARIO)
                .whenScenarioStateIs(STARTED)
                .willReturn(pathError("not_folder")));

        server.stubFor(post(urlPathEqualTo("/2/files/download"))
                .inScenario(SCENARIO)
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/octet-stream")
                        .withHeader("Dropbox-API-Result", FILE_METADATA.replaceAll("\\s+", ""))
                        .withBody(DropboxResource.FILE_CONTENT)));

        server.stubFor(post(urlPathEqualTo("/2/files/delete_v2"))
                .inScenario(SCENARIO)
                .whenScenarioStateIs(STARTED)
                .willSetStateTo(FILE_DELETED)
                .willReturn(json("{\"metadata\":" + FILE_METADATA + "}")));

        server.stubFor(post(urlPathEqualTo("/2/files/list_folder"))
                .inScenario(SCENARIO)
                .whenScenarioStateIs(FILE_DELETED)
                .willReturn(pathError("not_found")));

        server.stubFor(post(urlPathEqualTo("/2/files/download"))
                .inScenario(SCENARIO)
                .whenScenarioStateIs(FILE_DELETED)
                .willReturn(pathError("not_found")));
    }

    private static ResponseDefinitionBuilder json(String body) {
        return aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(body);
    }

    private static ResponseDefinitionBuilder pathError(String tag) {
        return json("""
                {"error_summary": "path/%s/", "error": {".tag": "path", "path": {".tag": "%s"}}}
                """.formatted(tag, tag))
                .withStatus(409);
    }

    @Override
    protected String getRecordTargetBaseUrl() {
        return null;
    }

    @Override
    protected boolean isMockingEnabled() {
        return !envVarsPresent(DROPBOX_ENV_VARS);
    }
}
