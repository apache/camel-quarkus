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
package org.apache.camel.quarkus.component.aws2.sts.it;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.aws2.sts.STS2Operations;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.services.sts.model.GetSessionTokenResponse;

@Path("/aws2-sts")
@ApplicationScoped
public class Aws2StsResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/session-token")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSessionToken() {
        GetSessionTokenResponse response = producerTemplate.requestBody(
                componentUri(STS2Operations.getSessionToken),
                null,
                GetSessionTokenResponse.class);

        Credentials credentials = response.credentials();
        return Response.ok(Map.of(
                "accessKeyId", credentials.accessKeyId(),
                "sessionToken", credentials.sessionToken())).build();
    }

    private String componentUri(STS2Operations operation) {
        return "aws2-sts://myaccount?operation=" + operation;
    }
}
