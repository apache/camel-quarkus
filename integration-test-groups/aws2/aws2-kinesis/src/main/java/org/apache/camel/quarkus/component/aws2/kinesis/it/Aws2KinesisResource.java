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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.aws2.kinesis.Kinesis2Constants;
import org.apache.camel.quarkus.test.support.aws2.BaseAws2Resource;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@Path("/aws2-kinesis")
@ApplicationScoped
public class Aws2KinesisResource extends BaseAws2Resource {

    private static final Logger log = Logger.getLogger(Aws2KinesisResource.class);

    @ConfigProperty(name = "aws-kinesis.stream-name")
    String streamName;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    @Named("aws2KinesisMessages")
    Queue<String> aws2KinesisMessages;

    public Aws2KinesisResource() {
        super("kinesis");
    }

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
        return "aws2-kinesis://" + streamName + "?useDefaultCredentialsProvider=" + isUseDefaultCredentials();
    }

}
