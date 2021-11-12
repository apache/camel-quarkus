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
package org.apache.camel.quarkus.component.salesforce;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.component.salesforce.SalesforceEndpointConfig;
import org.apache.camel.component.salesforce.api.dto.Attributes;
import org.apache.camel.component.salesforce.api.dto.CreateSObjectResult;
import org.apache.camel.component.salesforce.api.dto.GlobalObjects;
import org.apache.camel.component.salesforce.api.dto.Limits;
import org.apache.camel.component.salesforce.api.dto.RecentItem;
import org.apache.camel.component.salesforce.api.dto.RestResources;
import org.apache.camel.component.salesforce.api.dto.SObjectBasicInfo;
import org.apache.camel.component.salesforce.api.dto.SObjectDescription;
import org.apache.camel.component.salesforce.api.dto.Version;
import org.apache.camel.component.salesforce.api.dto.bulk.ContentType;
import org.apache.camel.component.salesforce.api.dto.bulk.JobInfo;
import org.apache.camel.component.salesforce.api.dto.bulk.OperationEnum;
import org.apache.camel.component.salesforce.api.utils.QueryHelper;
import org.apache.camel.component.salesforce.internal.dto.PushTopic;
import org.apache.camel.component.salesforce.internal.dto.QueryRecordsPushTopic;
import org.apache.camel.quarkus.component.salesforce.generated.Account;
import org.apache.camel.quarkus.component.salesforce.generated.QueryRecordsAccount;
import org.apache.camel.quarkus.component.salesforce.model.GlobalObjectsAndHeaders;
import org.apache.camel.spi.RouteController;

@Path("/salesforce")
public class SalesforceResource {

    @Inject
    FluentProducerTemplate template;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Inject
    CamelContext context;

    @Path("/document/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getDocument(@PathParam("id") String id) {
        return template.withBody(id)
                .withHeader(SalesforceEndpointConfig.SOBJECT_EXT_ID_NAME, "Name")
                .withHeader(SalesforceEndpointConfig.SOBJECT_NAME, "Document")
                .to("salesforce:getSObjectWithId?rawPayload=true")
                .request(String.class);
    }

    @Path("/account/query/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject getAccountByQueryRecords(@PathParam("id") String accountId) {
        String query = QueryHelper.queryToFetchFilteredFieldsOf(new Account(),
                sObjectField -> sObjectField.getName().equals("Id") || sObjectField.getName().equals("AccountNumber"));
        QueryRecordsAccount request = template
                .toF("salesforce:query?sObjectQuery=%s&sObjectClass=%s",
                        query + " WHERE Id = '" + accountId + "'",
                        QueryRecordsAccount.class.getName())
                .request(QueryRecordsAccount.class);
        return accountToJsonObject(request.getRecords().get(0));
    }

    @Path("/account")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public String createAccount(String accountName) {
        final Account account = new Account();
        account.setName(accountName);
        account.setAccountNumber(UUID.randomUUID().toString());

        CreateSObjectResult result = template.to("salesforce:createSObject?sObjectName=Account")
                .withBody(account)
                .request(CreateSObjectResult.class);

        return result.getId();
    }

    @Path("/account/{id}")
    @DELETE
    public Response deleteAccount(@PathParam("id") String accountId) {
        final Account account = new Account();
        account.setId(accountId);

        template.to("salesforce:deleteSObject")
                .withBody(account)
                .send();

        return Response.noContent().build();
    }

    @Path("/account/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject getAccountById(@PathParam("id") String accountId) {
        Account account = template
                .to("salesforce:getSObjectWithId?sObjectName=Account&sObjectIdName=Id&sObjectClass=" + Account.class.getName())
                .withBody(accountId)
                .request(Account.class);
        return accountToJsonObject(account);
    }

    @Path("/bulk")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject createJob() {
        JobInfo jobInfo = createJobInfo();
        jobInfo.setOperation(OperationEnum.INSERT);

        JobInfo result = template.to("salesforce:createJob").withBody(jobInfo).request(JobInfo.class);
        return jobInfoToJsonObject(result);
    }

    @Path("/bulk")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject abortJob(@QueryParam("jobId") String jobId) {
        JobInfo jobInfo = createJobInfo();
        jobInfo.setId(jobId);

        JobInfo result = template.to("salesforce:abortJob").withBody(jobInfo).request(JobInfo.class);
        return jobInfoToJsonObject(result);
    }

    @Path("/cdc/{action}")
    @POST
    public Response modifyCdcConsumerState(@PathParam("action") String action) throws Exception {
        RouteController controller = context.getRouteController();
        if (action.equals("start")) {
            controller.startRoute("cdc");
        } else if (action.equals("stop")) {
            controller.stopRoute("cdc");
        } else {
            throw new IllegalArgumentException("Unknown action: " + action);
        }
        return Response.ok().build();
    }

    @Path("/cdc")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCdcEvents() {
        return consumerTemplate.receiveBody("seda:events", 10000, Map.class);
    }

    @Path("sobjects/force-limit")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getSObjectsWithForceLimitInfo() {
        // Testing producer with headers
        Exchange exchange = template.to("salesforce:getGlobalObjects")
                .withHeader("Sforce-Limit-Info", Collections.singletonList("api-usage")).send();
        GlobalObjectsAndHeaders objectsAndHeaders = new GlobalObjectsAndHeaders(
                exchange.getMessage().getBody(GlobalObjects.class))
                        .withHeader("Sforce-Limit-Info", exchange.getMessage().getHeader("Sforce-Limit-Info", String.class));
        return objectsAndHeaders.getHeader("Sforce-Limit-Info");
    }

    @Path("versions")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public List<String> getListVersions() {
        List<Version> versions = template.to("salesforce:getVersions").request(List.class);
        return versions.stream()
                .map(Version::getVersion)
                .collect(Collectors.toList());
    }

    @Path("resources")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getListResources() {
        RestResources restResources = template.to("salesforce:getResources").request(RestResources.class);
        return restResources.getSobjects();
    }

    @Path("basic-info/account")
    @GET
    public JsonObject getAccountBasicInfo() {
        SObjectBasicInfo basicInfo = template.to("salesforce:getBasicInfo?sObjectName=Account").request(SObjectBasicInfo.class);
        return basicInfoToJsonObject(basicInfo);
    }

    @Path("describe/account")
    @GET
    public String getAccountDescription() {
        SObjectDescription sObjectDescription = template.to("salesforce:getDescription?sObjectName=Account")
                .request(SObjectDescription.class);
        return sObjectDescription.getName();
    }

    @Path("limits")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Integer> getLimits() {
        Limits limits = template.to("salesforce:limits").request(Limits.class);

        Map<String, Integer> limitInfo = new HashMap<>();
        for (Limits.Operation operation : Limits.Operation.values()) {
            Limits.Usage usage = limits.forOperation(operation.name());
            limitInfo.put(operation.name(), usage.getRemaining());
        }

        return limitInfo;
    }

    @Path("streaming")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getSubscribedObjects() {
        Account account = consumerTemplate.receiveBody("seda:CamelTestTopic", 10000, Account.class);
        return account.getName();
    }

    @Path("streaming/raw")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getRawSubscribedObjects() {
        return consumerTemplate.receiveBody("seda:RawPayloadCamelTestTopic", 10000, String.class);
    }

    @Path("/topic/{id}")
    @DELETE
    public Response deleteTopic(@PathParam("id") String topicId) {
        PushTopic topic = new PushTopic();
        topic.setId(topicId);

        template.to("salesforce:deleteSObject")
                .withBody(topic)
                .send();

        return Response.noContent().build();
    }

    @Path("/topic")
    @GET
    public String getTopicId() {
        QueryRecordsPushTopic queryRecordsPushTopic = template
                .to("salesforce:query?sObjectQuery=SELECT Id FROM PushTopic WHERE Name = 'CamelTestTopic'&"
                        + "sObjectClass=" + QueryRecordsPushTopic.class.getName())
                .request(QueryRecordsPushTopic.class);

        return queryRecordsPushTopic.getRecords().get(0).getId();
    }

    @Path("platform/event")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getPlatformEvent() {
        return consumerTemplate.receiveBody("salesforce:event/TestEvent__e?rawPayload=true", 10000, String.class);
    }

    private JsonObject accountToJsonObject(Account account) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("Id", account.getId());
        builder.add("AccountNumber", account.getAccountNumber());
        return builder.build();
    }

    private JobInfo createJobInfo() {
        JobInfo jobInfo = new JobInfo();
        jobInfo.setObject(Account.class.getSimpleName());
        jobInfo.setContentType(ContentType.CSV);
        return jobInfo;
    }

    private JsonObject jobInfoToJsonObject(JobInfo jobInfo) {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        objectBuilder.add("id", jobInfo.getId());
        objectBuilder.add("state", jobInfo.getState().value().toUpperCase(Locale.US));
        return objectBuilder.build();
    }

    private JsonObject basicInfoToJsonObject(SObjectBasicInfo basicInfo) {
        JsonObjectBuilder builder = Json.createObjectBuilder();

        List<RecentItem> recentItems = basicInfo.getRecentItems();
        JsonArrayBuilder recentItemsArrayBuilder = Json.createArrayBuilder();
        for (RecentItem recentItem : recentItems) {
            JsonObjectBuilder recentItemBuilder = Json.createObjectBuilder();
            recentItemBuilder.add("Id", recentItem.getId());
            recentItemBuilder.add("Name", recentItem.getName());

            Attributes attributes = recentItem.getAttributes();
            JsonObjectBuilder attributesObjectBuilder = Json.createObjectBuilder();
            attributesObjectBuilder.add("url", attributes.getUrl());
            recentItemBuilder.add("attributes", attributesObjectBuilder);

            recentItemsArrayBuilder.add(recentItemBuilder);
        }

        builder.add("recentItems", recentItemsArrayBuilder);

        return builder.build();
    }
}
