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
package org.apache.camel.quarkus.component.aws2.kinesis.it;

import java.io.IOException;
import java.net.URI;
import java.util.Queue;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.aws2.kinesis.Kinesis2Constants;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@Path("/aws2-kinesis")
@ApplicationScoped
public class Aws2KinesisResource {

    private static final Logger log = Logger.getLogger(Aws2KinesisResource.class);

    @ConfigProperty(name = "aws-kinesis.stream-name")
    String streamName;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    @Named("aws2KinesisMessages")
    Queue<String> aws2KinesisMessages;

    @Path("/send")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response send(String message) throws Exception {
        final String response = producerTemplate.requestBodyAndHeader(
                componentUri(),
                message,
                Kinesis2Constants.PARTITION_KEY,
                "foo-partition-key",
                String.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }

    @Path("/receive")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String receive() throws IOException {
        return aws2KinesisMessages.poll();
    }

    private String componentUri() {
        return "aws2-kinesis://" + streamName;
    }

}
