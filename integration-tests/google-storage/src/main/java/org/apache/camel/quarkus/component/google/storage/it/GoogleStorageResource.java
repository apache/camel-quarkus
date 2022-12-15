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
package org.apache.camel.quarkus.component.google.storage.it;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.CopyWriter;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import io.quarkus.arc.Arc;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.google.storage.GoogleCloudStorageConstants;
import org.apache.camel.component.google.storage.GoogleCloudStorageOperations;
import org.eclipse.microprofile.config.ConfigProvider;

@Path("/google-storage")
@ApplicationScoped
public class GoogleStorageResource {

    public static final String POLLING_ROUTE_NAME = "polling";
    public static final String DEST_BUCKET = "camel_quarkus_test_dest_bucket";
    public static final String TEST_BUCKET1 = "camel_quarkus_test_bucket1";
    public static final String TEST_BUCKET2 = "camel_quarkus_test_bucket2";
    public static final String TEST_BUCKET3 = "camel_quarkus_test_bucket3";

    public static final String DIRECT_POLLING = "direct:polling";

    public static final String PARAM_PORT = "org.apache.camel.quarkus.component.googlr.storage.it.GoogleStorageClientProducer_port";

    public static final String QUERY_OBJECT_NAME = "objectName";
    public static final String QUERY_BUCKET = "bucketName";
    public static final String QUERY_OPERATION = "operation";

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Inject
    CamelContext camelContext;

    @Named
    Storage storageClient() throws IOException {
        Storage storage;
        if (GoogleStorageHelper.usingMockBackend()) {
            String port = ConfigProvider.getConfig().getValue(GoogleStorageResource.PARAM_PORT, String.class);
            storage = StorageOptions.newBuilder()
                    .setHost("http://localhost:" + port)
                    .setProjectId("dummy-project-for-testing")
                    .build()
                    .getService();
        } else {
            storage = StorageOptions.getDefaultInstance().getService();
        }

        return storage;
    }

    @Path("/operation")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String operation(Map<String, Object> parameters,
            @QueryParam(QUERY_OPERATION) String operation,
            @QueryParam(QUERY_BUCKET) String bucketName) throws Exception {
        GoogleCloudStorageOperations op = GoogleCloudStorageOperations.valueOf(operation);
        String url = getBaseUrl(bucketName, "operation=" + op);
        if ((GoogleCloudStorageOperations.getObject.equals(op))) {
            return producerTemplate.requestBodyAndHeaders(url, null, parameters, String.class);
        }
        final Object response = producerTemplate.requestBodyAndHeaders(url, null, parameters, Object.class);
        if (response instanceof CopyWriter) {
            return new String(((CopyWriter) response).getResult().getContent());
        }
        if (response instanceof List) {
            List l = (List) response;
            return (String) l.stream().map(o -> {
                if (o instanceof Bucket) {
                    return ((Bucket) o).getName();
                }
                if (o instanceof Blob) {
                    return ((Blob) o).getName();
                }
                return "null";
            }).collect(Collectors.joining(","));
        }
        return String.valueOf(response);
    }

    @Path("/putObject")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response putObject(String body,
            @QueryParam(QUERY_BUCKET) String bucketName,
            @QueryParam(QUERY_OBJECT_NAME) String objectName) throws Exception {
        String url = getBaseUrl(bucketName, "autoCreateBucket=true");
        final Blob response = producerTemplate.requestBodyAndHeader(url,
                body,
                GoogleCloudStorageConstants.OBJECT_NAME, objectName, Blob.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response.getName())
                .build();
    }

    @Path("/getFromDirect")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String getFromDirect() {
        return consumerTemplate.receiveBody(GoogleStorageResource.DIRECT_POLLING,
                GoogleStorageHelper.usingMockBackend() ? 10000 : 5000, String.class);
    }

    private String getBaseUrl(String bucketName, String parameters) {
        return "google-storage://" + bucketName + "?" + parameters;
    }

    @Path("/deleteBuckets")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteBuckets() throws Exception {
        Storage client = Arc.container().instance(Storage.class).get();
        List<String> buckets = new LinkedList<>();
        for (Bucket bucket : client.list().iterateAll()) {
            buckets.add(bucket.getName());
        }
        if (!camelContext.getRouteController().getRouteStatus(POLLING_ROUTE_NAME).isStopped()) {
            buckets.remove(TEST_BUCKET3);
            buckets.remove(DEST_BUCKET);
        }

        buckets.stream().forEach(
                b -> {
                    for (Blob blob : client.list(b).iterateAll()) {
                        client.delete(blob.getBlobId());
                    }
                    client.delete(b);
                });
        return Response.ok().build();
    }

    @Path("/stopRoute")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response stopRoute() throws Exception {

        camelContext.getRouteController().stopRoute(POLLING_ROUTE_NAME);

        return Response.ok().build();
    }

}
