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

import com.sforce.eventbus.TestEvent__e;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
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
import org.apache.camel.quarkus.component.salesforce.model.TestEventPojo;
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

    @Path("/route/{routeId}/{action}")
    @POST
    public Response manageRoute(@PathParam("routeId") String routeId, @PathParam("action") String action)
            throws Exception {
        RouteController controller = context.getRouteController();
        if (action.equals("start")) {
            controller.startRoute(routeId);
        } else if (action.equals("stop")) {
            controller.stopRoute(routeId);
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
    @SuppressWarnings("unchecked")
    public Map<String, String> getListResources() {
        return template.to("salesforce:getResources").request(Map.class);
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
        return consumerTemplate.receiveBody("salesforce:subscribe:event/TestEvent__e?rawPayload=true", 10000, String.class);
    }

    @POST
    @Path("/publish/event/avro")
    public void pubSubPublishAvro(
            @QueryParam("createdBy") String createdBy,
            @QueryParam("createdDate") long createdDate,
            @QueryParam("testFieldValue") String testFieldValue) {
        TestEvent__e testEvent = TestEvent__e.newBuilder()
                .setCreatedDate(createdDate)
                .setCreatedById(createdBy)
                .setTestFieldC(testFieldValue)
                .build();

        template.to("direct:pubSubPublish")
                .withBody(List.of(testEvent))
                .request();
    }

    @POST
    @Path("/publish/event/generic/record")
    public void pubSubPublishGenericRecord(
            @QueryParam("createdBy") String createdBy,
            @QueryParam("createdDate") long createdDate,
            @QueryParam("testFieldValue") String testFieldValue) {
        GenericRecord record = new GenericRecordBuilder(TestEvent__e.getClassSchema())
                .set("CreatedDate", createdDate)
                .set("CreatedById", createdBy)
                .set("Test_Field__c", testFieldValue)
                .build();

        template.to("direct:pubSubPublish")
                .withBody(List.of(record))
                .request();
    }

    @POST
    @Path("/publish/event/json")
    @Consumes(MediaType.APPLICATION_JSON)
    public void pubSubPublishJson(String json) {
        template.to("direct:pubSubPublish")
                .withBody(List.of(json))
                .request();
    }

    @POST
    @Path("/publish/event/pojo")
    public void pubSubPublishPojo(
            @QueryParam("createdBy") String createdBy,
            @QueryParam("createdDate") long createdDate,
            @QueryParam("testFieldValue") String testFieldValue) {
        TestEventPojo pojo = new TestEventPojo();
        pojo.setCreatedDate(createdDate);
        pojo.setCreatedById(createdBy);
        pojo.setTest_Field__c(testFieldValue);

        template.to("direct:pubSubPublish")
                .withBody(List.of(pojo))
                .request();
    }

    @GET
    @Path("/subscribe/event/avro")
    @Produces(MediaType.APPLICATION_JSON)
    public Response pubSubSubscribeAvro() {
        TestEvent__e testEvent = consumerTemplate.receiveBody("seda:pubSubSubscribeAvro", 5000, TestEvent__e.class);
        if (testEvent == null) {
            return Response.serverError().entity("testEvent was null").build();
        }

        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("createdBy", testEvent.getCreatedById().toString());
        builder.add("createdDate", testEvent.getCreatedDate());
        builder.add("testFieldValue", testEvent.getTestFieldC().toString());

        return Response.ok().entity(builder.build()).build();
    }

    @GET
    @Path("/subscribe/event/generic/record")
    @Produces(MediaType.APPLICATION_JSON)
    public Response pubSubSubscribeGenericRecord() {
        GenericRecord record = consumerTemplate.receiveBody("seda:pubSubSubscribeGenericRecord", 5000, GenericRecord.class);
        if (record == null) {
            return Response.serverError().entity("record was null").build();
        }

        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("createdBy", record.get("CreatedById").toString());
        builder.add("createdDate", record.get("CreatedDate").toString());
        builder.add("testFieldValue", record.get("Test_Field__c").toString());

        return Response.ok().entity(builder.build()).build();
    }

    @GET
    @Path("/subscribe/event/json")
    @Produces(MediaType.APPLICATION_JSON)
    public String pubSubSubscribeJson() {
        return consumerTemplate.receiveBody("seda:pubSubSubscribeJson", 5000, String.class);
    }

    @GET
    @Path("/subscribe/event/pojo")
    @Produces(MediaType.APPLICATION_JSON)
    public Response pubSubSubscribePojo() {
        TestEventPojo testEvent = consumerTemplate.receiveBody("seda:pubSubSubscribePojo", 5000, TestEventPojo.class);
        if (testEvent == null) {
            return Response.serverError().entity("testEvent was null").build();
        }

        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("createdBy", testEvent.getCreatedById());
        builder.add("createdDate", testEvent.getCreatedDate());
        builder.add("testFieldValue", testEvent.getTest_Field__c());

        return Response.ok().entity(builder.build()).build();
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
