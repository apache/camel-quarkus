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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.Result;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.minio.MinioConstants;
import org.apache.camel.component.minio.MinioOperations;
import org.apache.camel.util.Pair;

@Path("/minio")
@ApplicationScoped
public class MinioResource {

    public static final String SERVER_ACCESS_KEY = "testAccessKey";
    public static final String SERVER_SECRET_KEY = "testSecretKey";
    private static final String URL_AUTH = "accessKey=" + SERVER_ACCESS_KEY + "&secretKey=RAW(" + SERVER_SECRET_KEY + ")";

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Inject
    CamelContext camelContext;

    @Path("/consumerWithClientCreation/{endpoint}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response consumerWithClientCreation(@PathParam("endpoint") String endpoint) {
        try {
            String url = "minioComponentWithoutClient://mycamel?endpoint=" + endpoint
                    + "&moveAfterRead=true&destinationBucketName=movedafterread&"
                    + URL_AUTH;

            return Response.ok().entity(consumerTemplate.receiveBody(url, 5000, String.class)).build();

        } catch (Exception e) {
            return Response.status(500).entity(e.getCause().getMessage()).build();
        }
    }

    @Path("/consumer")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String consumer() {

        final String message = consumerTemplate.receiveBody(
                "minio://mycamel?moveAfterRead=true&destinationBucketName=movedafterread",
                5000, String.class);
        return message;
    }

    @Path("/consumeAndMove/{removeHeader}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response consumeAndMove(@PathParam("removeHeader") boolean removeHeader) {

        final Exchange exchange = consumerTemplate.receive(
                "minio://movingfrombucket?deleteAfterRead=true",
                5000);

        Exchange exchangeToSend = exchange.copy();
        if (removeHeader) {
            exchangeToSend.getIn().removeHeader(MinioConstants.BUCKET_NAME);
        }

        producerTemplate.send("minio://movingtobucket", exchangeToSend);

        return Response.ok().build();
    }

    @Path("/getUsingPojo")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    public String getUsingPojo(String bucket,
            @QueryParam(MinioConstants.OBJECT_NAME) String objectName) {

        String endpoint = "minio:mycamel?pojoRequest=true&minioClient=#minioClient";

        GetObjectArgs.Builder body = GetObjectArgs.builder()
                .bucket(bucket)
                .object(objectName);

        Map<String, Object> headers = Collections.singletonMap(MinioConstants.MINIO_OPERATION, MinioOperations.getObject);

        return producerTemplate.requestBodyAndHeaders(endpoint, body, headers, String.class);
    }

    @Path("/operation")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    public Response operation(String body,
            //map of values in the format k1:v1,k2:v2,....
            @QueryParam("params") String parametersString,
            //if true result string starts with the headers from the received exchange
            @QueryParam("returnHeaders") boolean returnHeaders) {

        //transform string to map to avoid jackson
        Map<String, Object> headers = deserializeMap(parametersString);

        String endpoint = String.format("minio:%s?accessKey=" + SERVER_ACCESS_KEY
                + "&secretKey=RAW(" + SERVER_SECRET_KEY + ")", headers.getOrDefault("bucket", "mycamel"));
        headers.remove("bucket");

        if (headers.containsKey("autoCreateBucket")) {
            endpoint = endpoint + "&autoCreateBucket=" + headers.remove("autoCreateBucket");
        }

        MinioOperations op = (MinioOperations) headers.getOrDefault(MinioConstants.MINIO_OPERATION, null);
        Integer length = (Integer) headers.getOrDefault(MinioConstants.LENGTH, null);
        Integer offset = (Integer) headers.getOrDefault(MinioConstants.OFFSET, null);

        if (op == MinioOperations.getObject) {
            try {
                return Response.ok().entity(producerTemplate.requestBodyAndHeaders(endpoint, body, headers, String.class))
                        .build();
            } catch (Exception e) {
                return Response.status(500).entity(e.getCause().getMessage())
                        .build();
            }
        }

        Iterable objectList;
        var sb = new StringBuilder();
        var errorSB = new StringBuilder();
        try {
            Exchange exchange = producerTemplate.request(endpoint, e -> {
                e.getIn().setHeaders(headers);
                e.getIn().setBody(body);
            });
            if (returnHeaders) {
                sb.append("headers[")
                        .append(exchange.getIn().getHeaders().entrySet().stream().map(e -> e.getKey() + ":" + e.getValue())
                                .collect(Collectors.joining(",")))
                        .append("]");
            }
            objectList = exchange.getIn(Iterable.class);
        } catch (Exception e) {
            return Response.status(500)
                    .entity(e.getMessage())
                    .build();
        }
        formatResult(length, offset, objectList, sb, errorSB);
        var respBuilder = errorSB.length() > 0 ? Response.status(500).entity(errorSB.toString())
                : Response.ok().entity(sb.toString());
        return respBuilder.build();
    }

    private void formatResult(Integer length, Integer offset, Iterable objectList, StringBuilder sb, StringBuilder errorSB) {
        objectList.forEach(r -> {
            try {
                if (r instanceof Result) {
                    Object o = ((Result) r).get();
                    if (o instanceof Item) {
                        sb.append("item: ").append(((Item) o).objectName());
                    } else {
                        sb.append(o);
                    }
                } else if (r instanceof Bucket) {
                    sb.append("bucket: ").append(((Bucket) r).name());
                } else if (r instanceof GetObjectResponse) {
                    if (length != null && offset != null) {
                        byte[] bytes = new byte[length];
                        ((GetObjectResponse) r).read(bytes, 0, length - offset);
                        sb.append(new String(bytes, StandardCharsets.UTF_8));
                    } else {
                        errorSB.append("Offset and length is required!");
                    }

                } else {
                    sb.append(r);
                }
                sb.append(", ");
            } catch (Exception e) {
                errorSB.append(e);
            }
        });
    }

    private Map<String, Object> deserializeMap(String parametersString) {
        return Arrays.stream(parametersString.split(","))
                .map(s -> new Pair(s.split(":")[0], s.split(":")[1]))
                .map(p -> {
                    switch (p.getLeft().toString()) {
                    case MinioConstants.OFFSET:
                    case MinioConstants.LENGTH:
                        return new Pair<>(p.getLeft(), Integer.parseInt(p.getRight().toString()));
                    case MinioConstants.MINIO_OPERATION:
                        return new Pair<>(p.getLeft(), MinioOperations.valueOf(p.getRight().toString()));
                    default:
                        return p;
                    }
                })
                .collect(Collectors.toMap(p -> p.getLeft().toString(), p -> p.getRight()));
    }

}
