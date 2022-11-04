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
package org.apache.camel.quarkus.component.google.bigquery.it;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.NoCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.JobId;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.google.bigquery.GoogleBigQueryConnectionFactory;
import org.apache.camel.component.google.bigquery.GoogleBigQueryConstants;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/google-bigquery")
public class GoogleBigqueryResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    @ConfigProperty(name = "project.id")
    String projectId;

    @Inject
    @ConfigProperty(name = "google-bigquery.test_dataset")
    String datasetName;

    @Path("/insertMap")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response insertMap(
            @QueryParam("headerKey") String headerKey,
            @QueryParam("headerValue") String headerValue,
            @QueryParam("tableName") String tableName,
            Map<String, String> tableData) {

        return insert(tableName, tableData, headerKey, headerValue);
    }

    @Path("/insertList")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response insertList(
            @QueryParam("headerKey") String headerKey,
            @QueryParam("headerValue") String headerValue,
            @QueryParam("tableName") String tableName,
            List tableData) {

        return insert(tableName, tableData, headerKey, headerValue);
    }

    private Response insert(String tableName, Object tableData, String headerKey, Object headerValue) {
        if (headerKey == null) {
            producerTemplate.requestBody("google-bigquery:" + projectId + ":" + datasetName + ":" + tableName
                    + "?connectionFactory=#connectionFactory", tableData);
        } else {
            producerTemplate.requestBodyAndHeaders("google-bigquery:" + projectId + ":" + datasetName + ":" + tableName,
                    tableData, Collections.singletonMap(headerKey, headerValue));
        }
        return Response.created(URI.create("https://camel.apache.org")).build();
    }

    @Path("/executeSql")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Long executeSql(Map<String, String> headers, @QueryParam("sql") String sql, @QueryParam("file") boolean file,
            @QueryParam("body") String body)
            throws IOException {
        String uri = "google-bigquery-sql://" + projectId + ":";
        Map<String, Object> typedHeaders = headers == null ? Collections.emptyMap()
                : headers.entrySet().stream().collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> {
                            if (GoogleBigQueryConstants.JOB_ID.equals(e.getKey())) {
                                return JobId.newBuilder().setJob(e.getValue()).setProject(projectId).build();
                            }
                            try {
                                //pass integer values as Integer types
                                return Integer.valueOf(e.getValue());
                            } catch (NumberFormatException ex) {
                                //fallback to the String type
                                return e.getValue();
                            }
                        }));

        if (file) {
            java.nio.file.Path path = Files.createTempDirectory("bigquery");
            java.nio.file.Path sqlFile = Files.createTempFile(path, "bigquery", ".sql");
            Files.write(sqlFile, sql.getBytes(StandardCharsets.UTF_8));

            uri = uri + "file:" + sqlFile.toAbsolutePath();
        } else {
            uri = uri + sql;
        }

        Map<String, Object> bodyMap = new HashMap<>();
        if (body != null) {
            bodyMap.put(body, typedHeaders.get(body));
            typedHeaders.remove(body);
        }

        return producerTemplate.requestBodyAndHeaders(uri, bodyMap, typedHeaders,
                Long.class);
    }

    @Produces
    @Singleton
    @Named("connectionFactory")
    GoogleBigQueryConnectionFactory createConnectionFactory() {

        return new GoogleBigQueryConnectionFactory() {
            @Override
            public synchronized BigQuery getDefaultClient() throws Exception {
                final String host = ConfigProvider.getConfig().getOptionalValue("wiremock.url", String.class)
                        .orElse(null);
                final String credentialsPath = ConfigProvider.getConfig()
                        .getOptionalValue("google.credentialsPath", String.class)
                        .orElse(null);

                BigQueryOptions.Builder builder = BigQueryOptions.newBuilder().setProjectId(projectId);

                if (host != null) {
                    builder.setHost(host)
                            .setLocation(host);
                }

                if (credentialsPath == null) {
                    builder.setCredentials(NoCredentials.getInstance());
                } else {
                    GoogleCredentials credentials;
                    try (FileInputStream serviceAccountStream = new FileInputStream(credentialsPath)) {
                        credentials = ServiceAccountCredentials.fromStream(serviceAccountStream);
                    }
                    builder.setCredentials(credentials);
                }

                return builder.build()
                        .getService();
            }
        };
    }
}
