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

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.aws2.s3.AWS2S3Constants;
import org.apache.camel.component.aws2.s3.AWS2S3Operations;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.s3.model.S3Object;

@Path("/aws2")
@ApplicationScoped
public class Aws2S3Resource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @ConfigProperty(name = "aws-s3.bucket-name")
    String bucketName;

    @Path("s3/object/{key}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response post(String message, @PathParam("key") String key) throws Exception {
        producerTemplate.sendBodyAndHeader(componentUri(),
                message,
                AWS2S3Constants.KEY,
                key);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @Path("s3/object/{key}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get(@PathParam("key") String key) throws Exception {
        return producerTemplate.requestBodyAndHeader(
                componentUri(AWS2S3Operations.getObject),
                null,
                AWS2S3Constants.KEY,
                key,
                String.class);
    }

    @Path("s3/poll-object/{key}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String pollObject(@PathParam("key") String key) throws Exception {
        return consumerTemplate.receiveBody(componentUri() + "?fileName=" + key, 10000, String.class);
    }

    @Path("s3/object/{key}")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response read(@PathParam("key") String key) throws Exception {
        producerTemplate.sendBodyAndHeader(
                componentUri(AWS2S3Operations.deleteObject),
                null,
                AWS2S3Constants.KEY,
                key);
        return Response.noContent().build();
    }

    @Path("s3/object-keys")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> objectKey() throws Exception {
        final List<S3Object> objects = (List<S3Object>) producerTemplate.requestBody(
                componentUri(AWS2S3Operations.listObjects),
                null,
                List.class);
        return objects.stream().map(S3Object::key).collect(Collectors.toList());
    }

    private String componentUri(final AWS2S3Operations operation) {
        return String.format("aws2-s3://%s?operation=%s", bucketName, operation);
    }

    private String componentUri() {
        return String.format("aws2-s3://%s", bucketName);
    }

}
