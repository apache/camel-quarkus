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
package org.apache.camel.quarkus.component.minio.it;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.Result;
import io.minio.errors.MinioException;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.minio.MinioConstants;
import org.apache.camel.component.minio.MinioOperations;

@Path("/minio")
@ApplicationScoped
public class MinioResource {
    private static final long PART_SIZE = 50 * 1024 * 1024;

    public static final String SERVER_ACCESS_KEY = "testAccessKey";
    public static final String SERVER_SECRET_KEY = "testSecretKey";
    public static final String PARAM_SERVER_HOST = MinioResource.class.getSimpleName() + "_serverHost";
    public static final String PARAM_SERVER_PORT = MinioResource.class.getSimpleName() + "_serverPort";

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Inject
    MinioClient minioClient;

    @Path("/consumer")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String consumer() {

        String serverUrl = "http://" + System.getProperty(PARAM_SERVER_HOST) + ":" + System.getProperty(PARAM_SERVER_PORT);

        final String message = consumerTemplate.receiveBody(
                "minio://mycamel?moveAfterRead=true&destinationBucketName=camel-kafka-connector&autoCreateBucket=true"
                        + "&accessKey=" + SERVER_ACCESS_KEY
                        + "&secretKey=RAW(" + SERVER_SECRET_KEY + ")"
                        + "&endpoint=" + serverUrl
                        + "&secure=true",
                5000, String.class);
        return message;
    }

    @Path("/operation")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    public String operation(String body,
            @QueryParam(MinioConstants.MINIO_OPERATION) String operation,
            @QueryParam(MinioConstants.OBJECT_NAME) String objectName,
            @QueryParam(MinioConstants.DESTINATION_OBJECT_NAME) String destinationObjectName,
            @QueryParam(MinioConstants.DESTINATION_BUCKET_NAME) String destinationBucketName) {

        String serverUrl = "http://" + System.getProperty(PARAM_SERVER_HOST) + ":" + System.getProperty(PARAM_SERVER_PORT);
        String endpoint = "minio:mycamel?accessKey=" + SERVER_ACCESS_KEY
                + "&secretKey=RAW(" + SERVER_SECRET_KEY + ")"
                + "&endpoint=" + serverUrl;

        MinioOperations op = (operation != "" && !"".equals(operation) ? MinioOperations.valueOf(operation) : null);

        Map<String, Object> headers = new HashMap<>();
        if (op != null) {
            headers.put(MinioConstants.MINIO_OPERATION, op);
        }
        if (objectName != null) {
            headers.put(MinioConstants.OBJECT_NAME, objectName);
        }
        if (destinationObjectName != null) {
            headers.put(MinioConstants.DESTINATION_OBJECT_NAME, destinationObjectName);
        }
        if (destinationBucketName != null) {
            headers.put(MinioConstants.DESTINATION_BUCKET_NAME, destinationBucketName);
        }

        if (op == MinioOperations.getObject) {
            return producerTemplate.requestBodyAndHeaders(endpoint, body, headers, String.class);
        }

        final Iterable objectList = producerTemplate.requestBodyAndHeaders(endpoint, body, headers, Iterable.class);
        StringBuilder sb = new StringBuilder();
        StringBuilder errorSB = new StringBuilder();
        objectList.forEach(r -> {
            try {
                if (r instanceof Result) {
                    Object o = ((Result) r).get();
                    if (o instanceof Item) {
                        sb.append("item: ").append(((Item) o).objectName());
                    } else {
                        sb.append(o);
                    }
                }
                if (r instanceof Bucket) {
                    sb.append("bucket: ").append(((Bucket) r).name());
                } else {
                    sb.append(r);
                }
                sb.append(", ");
            } catch (Exception e) {
                errorSB.append(e.toString());
            }
        });
        return errorSB.length() > 0 ? errorSB.toString() : sb.toString();
    }

    @Path("/getUsingPojo")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    public String getUsingPojo(String bucket,
            @QueryParam(MinioConstants.OBJECT_NAME) String objectName) {

        String serverUrl = "http://" + System.getProperty(PARAM_SERVER_HOST) + ":" + System.getProperty(PARAM_SERVER_PORT);
        String endpoint = "minio:mycamel?accessKey=" + SERVER_ACCESS_KEY
                + "&secretKey=RAW(" + SERVER_SECRET_KEY + ")"
                + "&endpoint=" + serverUrl
                + "&pojoRequest=true";

        GetObjectArgs.Builder body = GetObjectArgs.builder()
                .bucket(bucket)
                .object(objectName);

        Map<String, Object> headers = Collections.singletonMap(MinioConstants.MINIO_OPERATION, MinioOperations.getObject);

        return producerTemplate.requestBodyAndHeaders(endpoint, body, headers, String.class);

    }

    @Path("/initBucket")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void initBucket(String bucketName) throws Exception {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    @Path("/putObject")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public void putObject(String content,
            @QueryParam(MinioConstants.OBJECT_NAME) String objectName,
            @QueryParam(MinioConstants.BUCKET_NAME) String bucketName) throws Exception {
        try (InputStream is = new ByteArrayInputStream((content.getBytes()))) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .contentType("text/xml")
                            .stream(is, -1, PART_SIZE)
                            .build());
        } catch (MinioException | GeneralSecurityException | IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
