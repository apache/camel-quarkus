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
package org.apache.camel.quarkus.component.aws.cloudtrail.it;

import java.net.URI;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.cloudtrail.CloudTrailClient;
import software.amazon.awssdk.services.cloudtrail.model.CreateTrailRequest;
import software.amazon.awssdk.services.cloudtrail.model.CreateTrailResponse;
import software.amazon.awssdk.services.cloudtrail.model.DeleteTrailRequest;
import software.amazon.awssdk.services.cloudtrail.model.Event;

@Path("/aws-cloudtrail")
@ApplicationScoped
public class AwsCloudtrailResource {

    @Inject
    CloudTrailClient cloudTrailClient;

    @Inject
    @Named("cloudtrailEvents")
    List<Event> cloudtrailEvents;

    @ConfigProperty(name = "aws.cloudtrail.s3.bucket.name")
    String s3BucketName;

    @Path("/trail/{trailName}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response createTrail(@PathParam("trailName") String trailName) throws Exception {
        CreateTrailRequest request = CreateTrailRequest.builder()
                .name(trailName)
                .s3BucketName(s3BucketName)
                .build();

        CreateTrailResponse response = cloudTrailClient.createTrail(request);

        return Response.created(new URI("https://camel.apache.org/")).entity(response.trailARN()).build();
    }

    @Path("/trail/{trailName}")
    @DELETE
    public void deleteTrail(@PathParam("trailName") String trailName) {
        DeleteTrailRequest request = DeleteTrailRequest.builder()
                .name(trailName)
                .build();

        cloudTrailClient.deleteTrail(request);
    }

    @Path("/consumer/events")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConsumedEvents() {
        // Return the count of events consumed by the Camel CloudTrail route
        return Response.ok(cloudtrailEvents.size()).build();
    }
}
