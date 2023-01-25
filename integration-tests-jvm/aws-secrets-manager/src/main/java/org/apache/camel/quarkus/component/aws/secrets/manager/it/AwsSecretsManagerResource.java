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
package org.apache.camel.quarkus.component.aws.secrets.manager.it;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.aws.secretsmanager.SecretsManagerOperations;
import org.apache.camel.util.CollectionHelper;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretResponse;
import software.amazon.awssdk.services.secretsmanager.model.DeleteSecretResponse;
import software.amazon.awssdk.services.secretsmanager.model.DescribeSecretResponse;
import software.amazon.awssdk.services.secretsmanager.model.ListSecretsResponse;
import software.amazon.awssdk.services.secretsmanager.model.ReplicateSecretToRegionsResponse;
import software.amazon.awssdk.services.secretsmanager.model.RestoreSecretResponse;
import software.amazon.awssdk.services.secretsmanager.model.RotateSecretResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretListEntry;
import software.amazon.awssdk.services.secretsmanager.model.UpdateSecretResponse;

@Path("/aws-secrets-manager")
@ApplicationScoped
public class AwsSecretsManagerResource {

    private static final Logger LOG = Logger.getLogger(AwsSecretsManagerResource.class);

    private static final String COMPONENT_AWS_SECRETS_MANAGER = "aws-secrets-manager";

    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/operation/{operation}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(@PathParam("operation") String operation, @QueryParam("body") String body, Map<String, Object> headers)
            throws Exception {

        Exchange ex = producerTemplate.send("aws-secrets-manager://test?operation=" + operation, e -> {
            e.getIn().setHeaders(headers);
            e.getIn().setBody(body);
        });

        Object result;
        switch (SecretsManagerOperations.valueOf(operation)) {
        case createSecret:
            result = ex.getIn().getBody(CreateSecretResponse.class).arn();
            break;
        case listSecrets:
            //returns map with ar as a key and a flag, whether is deleted or not
            result = ex.getIn().getBody(ListSecretsResponse.class).secretList().stream()
                    .collect(Collectors.toMap(SecretListEntry::arn, e -> e.deletedDate() == null ? false : true));
            break;
        case describeSecret:
            DescribeSecretResponse response = ex.getIn().getBody(DescribeSecretResponse.class);
            result = CollectionHelper.mapOf("sdkHttpSuccessful", response.sdkHttpResponse().isSuccessful(),
                    "name", response.name());
            break;
        case getSecret:
            result = ex.getIn().getBody(String.class);
            break;
        case deleteSecret:
            result = ex.getIn().getBody(DeleteSecretResponse.class).sdkHttpResponse().isSuccessful();
            break;
        case updateSecret:
            result = ex.getIn().getBody(UpdateSecretResponse.class).sdkHttpResponse().isSuccessful();
            break;
        case rotateSecret:
            result = ex.getIn().getBody(RotateSecretResponse.class).sdkHttpResponse().isSuccessful();
            break;
        case replicateSecretToRegions:
            result = ex.getIn().getBody(ReplicateSecretToRegionsResponse.class).sdkHttpResponse().isSuccessful();
            break;
        case restoreSecret:
            result = ex.getIn().getBody(RestoreSecretResponse.class).sdkHttpResponse().isSuccessful();
            break;
        default:
            return Response.status(500).build();
        }

        return Response.created(new URI("https://camel.apache.org/")).entity(result).build();
    }
}
