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
package org.apache.camel.quarkus.component.aws2;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.aws2.s3.AWS2S3Component;
import org.apache.camel.component.aws2.s3.AWS2S3ComponentConfigurer;
import org.apache.camel.component.aws2.s3.AWS2S3Constants;
import org.apache.camel.component.aws2.s3.AWS2S3Operations;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Object;

@Path("/aws2")
@ApplicationScoped
public class Aws2S3Resource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Inject
    S3Client s3Client;

    @ConfigProperty(name = "aws-s3.bucket-name")
    String bucketName;

    @Path("{service}/object/{key}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response post(String message,
            @PathParam("service") String service, @PathParam("key") String key) throws Exception {
        producerTemplate.sendBodyAndHeader(componentUri(service),
                message,
                AWS2S3Constants.KEY,
                key);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @Path("{service}/object/{key}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get(@PathParam("service") String service, @PathParam("key") String key,
            @QueryParam("bucket") String bucket) throws Exception {
        if (bucket == null) {
            bucket = bucketName;
        }

        return producerTemplate.requestBodyAndHeader(
                componentUri(service, bucket, AWS2S3Operations.getObject),
                null,
                AWS2S3Constants.KEY,
                key,
                String.class);
    }

    @Path("{service}/poll-object/{key}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String pollObject(@PathParam("service") String service, @PathParam("key") String key) throws Exception {
        return consumerTemplate.receiveBody(componentUri(service) + "?fileName=" + key, 10000, String.class);
    }

    @Path("{service}/object/{key}")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response read(@PathParam("service") String service, @PathParam("key") String key) throws Exception {
        producerTemplate.sendBodyAndHeader(
                componentUri(service, AWS2S3Operations.deleteObject),
                null,
                AWS2S3Constants.KEY,
                key);
        return Response.noContent().build();
    }

    @Path("{service}/object-keys")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> objectKey(@PathParam("service") String service, @QueryParam("bucket") String bucket) throws Exception {
        if (bucket == null) {
            bucket = bucketName;
        }

        final List<S3Object> objects = (List<S3Object>) producerTemplate.requestBody(
                componentUri(service, bucket, AWS2S3Operations.listObjects) + "&autoCreateBucket=true",
                null,
                List.class);
        return objects.stream().map(S3Object::key).collect(Collectors.toList());
    }

    @Path("{service}/upload/{key}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String upload(@PathParam("service") String service, @PathParam("key") String key, String content) throws Exception {
        File file = File.createTempFile("aws2-", ".tmp");
        Files.writeString(file.toPath(), content, StandardOpenOption.WRITE);
        int partSize = 5 * 1024 * 1024;

        producerTemplate.sendBodyAndHeader(
                componentUri(service) + "?multiPartUpload=true&partSize=" + partSize + "&autoCreateBucket=true",
                file,
                AWS2S3Constants.KEY,
                key);

        return key;
    }

    @Path("{service}/copy/{key}")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response copyObject(@PathParam("service") String service, @PathParam("key") String key,
            @FormParam("dest_key") String dest_key, @FormParam("dest_bucket") String dest_bucket) {
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put(AWS2S3Constants.KEY, key);
        headers.put(AWS2S3Constants.DESTINATION_KEY, dest_key);
        headers.put(AWS2S3Constants.BUCKET_DESTINATION_NAME, dest_bucket);

        producerTemplate.sendBodyAndHeaders(
                componentUri(service, AWS2S3Operations.copyObject),
                null, headers);

        return Response.noContent().build();
    }

    @Path("{service}/bucket")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> listBuckets(@PathParam("service") String service) throws Exception {
        List<Bucket> buckets = (List<Bucket>) producerTemplate.requestBody(
                componentUri(service, AWS2S3Operations.listBuckets),
                null,
                List.class);
        return buckets.stream().map(Bucket::name).collect(Collectors.toList());
    }

    @Path("{service}/bucket/{name}")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteBucket(@PathParam("service") String service, @PathParam("name") String bucketName) {
        try {
            producerTemplate.sendBodyAndHeader(
                    componentUri(service, bucketName, AWS2S3Operations.deleteBucket),
                    null,
                    AWS2S3Constants.BUCKET_NAME,
                    bucketName);
        } catch (NoSuchBucketException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.noContent().build();
    }

    @Path("{service}/downloadlink/{key}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String downloadLink(@PathParam("service") String service, @PathParam("key") String key,
            @QueryParam("bucket") String bucket) {
        if (bucket == null) {
            bucket = bucketName;
        }

        String link = producerTemplate.requestBodyAndHeader(
                componentUri(service, bucket, AWS2S3Operations.createDownloadLink),
                null,
                AWS2S3Constants.KEY,
                key,
                String.class);

        return link;
    }

    @Path("{service}/object/range/{key}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String objectRange(@PathParam("service") String service, @PathParam("key") String key,
            @QueryParam("start") Integer start,
            @QueryParam("end") Integer end) throws Exception {
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put(AWS2S3Constants.KEY, key);
        headers.put(AWS2S3Constants.RANGE_START, start);
        headers.put(AWS2S3Constants.RANGE_END, end);

        ResponseInputStream<GetObjectResponse> s3Object = producerTemplate.requestBodyAndHeaders(
                componentUri(service, AWS2S3Operations.getObjectRange) + "&autoCreateBucket=false",
                null,
                headers,
                ResponseInputStream.class);

        StringBuilder textBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(
                new InputStreamReader(s3Object, Charset.forName(StandardCharsets.UTF_8.name())))) {
            int c = 0;
            while ((c = reader.read()) != -1) {
                textBuilder.append((char) c);
            }
        }

        return textBuilder.toString();
    }

    private String componentUri(String service, String bucketName, final AWS2S3Operations operation) {
        return String.format("aws2-%s://%s?operation=%s", service, bucketName, operation);
    }

    private String componentUri(String service, final AWS2S3Operations operation) {
        return componentUri(service, bucketName, operation);
    }

    private String componentUri(String serivce) {
        return String.format("aws2-%s://%s", serivce, bucketName);
    }

    @Named("aws2-s3-quarkus")
    public AWS2S3Component aws2S3ComponentQuarkus() {
        return new AWS2S3Component();
    }

    @Named("aws2-s3-quarkus-component")
    public AWS2S3ComponentConfigurer aws2S3ComponentConfigurer() {
        return new AWS2S3ComponentConfigurer();
    }

}
