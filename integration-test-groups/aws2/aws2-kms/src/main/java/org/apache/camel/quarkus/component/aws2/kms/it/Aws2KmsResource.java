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
package org.apache.camel.quarkus.component.aws2.kms.it;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.aws2.kms.KMS2Constants;
import org.apache.camel.component.aws2.kms.KMS2Operations;
import org.apache.camel.quarkus.test.support.aws2.BaseAws2Resource;
import software.amazon.awssdk.services.kms.model.CreateKeyResponse;
import software.amazon.awssdk.services.kms.model.DescribeKeyResponse;
import software.amazon.awssdk.services.kms.model.KeyListEntry;
import software.amazon.awssdk.services.kms.model.ListKeysResponse;
import software.amazon.awssdk.services.kms.model.ScheduleKeyDeletionResponse;

@Path("/aws2-kms")
@ApplicationScoped
public class Aws2KmsResource extends BaseAws2Resource {

    @Inject
    ProducerTemplate producerTemplate;

    public Aws2KmsResource() {
        super("kms");
    }

    @Path("/keys")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String createKey() {
        CreateKeyResponse response = producerTemplate.requestBodyAndHeaders(
                componentUri(KMS2Operations.createKey),
                null,
                Collections.emptyMap(),
                CreateKeyResponse.class);
        return response.keyMetadata().keyId();
    }

    @Path("/keys")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> listKeys() {
        ListKeysResponse response = producerTemplate.requestBody(
                componentUri(KMS2Operations.listKeys),
                null,
                ListKeysResponse.class);
        return response.keys().stream().map(KeyListEntry::keyId).collect(Collectors.toList());
    }

    @Path("/keys/{keyId}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String describeKey(@PathParam("keyId") String keyId) {
        DescribeKeyResponse response = producerTemplate.requestBodyAndHeaders(
                componentUri(KMS2Operations.describeKey),
                null,
                Map.of(KMS2Constants.KEY_ID, keyId),
                DescribeKeyResponse.class);
        return response.keyMetadata().keyState().toString();
    }

    @Path("/keys/{keyId}/disable")
    @POST
    public void disableKey(@PathParam("keyId") String keyId) {
        producerTemplate.requestBodyAndHeaders(
                componentUri(KMS2Operations.disableKey),
                null,
                Map.of(KMS2Constants.KEY_ID, keyId));
    }

    @Path("/keys/{keyId}/enable")
    @POST
    public void enableKey(@PathParam("keyId") String keyId) {
        producerTemplate.requestBodyAndHeaders(
                componentUri(KMS2Operations.enableKey),
                null,
                Map.of(KMS2Constants.KEY_ID, keyId));
    }

    @Path("/keys/{keyId}")
    @DELETE
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String scheduleKeyDeletion(@PathParam("keyId") String keyId, String pendingWindowInDays) {
        Map<String, Object> headers = pendingWindowInDays == null || pendingWindowInDays.isBlank()
                ? Map.of(KMS2Constants.KEY_ID, keyId)
                : Map.of(KMS2Constants.KEY_ID, keyId,
                        KMS2Constants.PENDING_WINDOW_IN_DAYS, Integer.parseInt(pendingWindowInDays));

        ScheduleKeyDeletionResponse response = producerTemplate.requestBodyAndHeaders(
                componentUri(KMS2Operations.scheduleKeyDeletion),
                null,
                headers,
                ScheduleKeyDeletionResponse.class);
        return response.keyId();
    }

    private String componentUri(KMS2Operations operation) {
        return "aws2-kms:cq-test?operation=" + operation.name()
                + "&useDefaultCredentialsProvider=" + isUseDefaultCredentials();
    }
}
