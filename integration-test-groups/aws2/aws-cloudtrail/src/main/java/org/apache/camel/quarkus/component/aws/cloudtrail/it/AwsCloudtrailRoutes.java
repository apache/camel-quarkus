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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.cloudtrail.CloudtrailConstants;
import software.amazon.awssdk.services.cloudtrail.model.Event;

@ApplicationScoped
public class AwsCloudtrailRoutes extends RouteBuilder {

    @Inject
    @Named("cloudtrailEvents")
    List<Event> cloudtrailEvents;

    @Override
    public void configure() {
        from("aws-cloudtrail:cloudtrail-events"
                + "?cloudTrailClient=#cloudTrailClient"
                + "&maxResults=10"
                + "&delay=1000"
                + "&initialDelay=0")
                .process(e -> {
                    // CloudTrail consumer sets message body to cloudTrailEvent JSON bytes
                    // Event details are in headers
                    String eventId = e.getMessage().getHeader(CloudtrailConstants.EVENT_ID, String.class);
                    String eventName = e.getMessage().getHeader(CloudtrailConstants.EVENT_NAME, String.class);
                    String eventSource = e.getMessage().getHeader(CloudtrailConstants.EVENT_SOURCE, String.class);
                    String username = e.getMessage().getHeader(CloudtrailConstants.USERNAME, String.class);

                    // Create Event object from headers for collection
                    Event event = Event.builder()
                            .eventId(eventId)
                            .eventName(eventName)
                            .eventSource(eventSource)
                            .username(username)
                            .build();

                    cloudtrailEvents.add(event);
                });
    }

    static class Producers {
        @Singleton
        @Produces
        @Named("cloudtrailEvents")
        List<Event> cloudtrailEvents() {
            return new CopyOnWriteArrayList<>();
        }
    }
}
