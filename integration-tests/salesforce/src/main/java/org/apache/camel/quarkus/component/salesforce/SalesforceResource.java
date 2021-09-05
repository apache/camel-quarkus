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
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.json.Json;
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
import org.apache.camel.component.salesforce.api.dto.CreateSObjectResult;
import org.apache.camel.component.salesforce.api.dto.GlobalObjects;
import org.apache.camel.component.salesforce.api.dto.Limits;
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
    public Object getDocument(@PathParam("id") String id) {
        return template.withBody(id)
                .withHeader(SalesforceEndpointConfig.SOBJECT_EXT_ID_NAME, "Name")
                .withHeader(SalesforceEndpointConfig.SOBJECT_NAME, "Document")
                .to("salesforce:getSObjectWithId?rawPayload=true")
                .request();
    }

    @Path("/account")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Account getAccount() {
        QueryRecordsAccount request = template
                .toF("salesforce:query?sObjectQuery=SELECT Id,AccountNumber from Account LIMIT 1&sObjectClass=%s",
                        QueryRecordsAccount.class.getName())
                .request(QueryRecordsAccount.class);
        return request.getRecords().get(0);
    }

    @Path("/account/query")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Account getAccountByQueryHelper() {
        String generatedQuery = QueryHelper.queryToFetchAllFieldsOf(new Account());
        QueryRecordsAccount request = template
                .toF("salesforce:query?sObjectQuery=%s LIMIT 1&sObjectClass=%s",
                        generatedQuery,
                        QueryRecordsAccount.class.getName())
                .request(QueryRecordsAccount.class);
        return request.getRecords().get(0);
    }

    @Path("/account")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public String createAccount(String accountName) {
        final Account account = new Account();
        account.setName(accountName);

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
    public Account getAccountById(@PathParam("id") String accountId) {
        Account account = template
                .to("salesforce:getSObjectWithId?sObjectName=Account&sObjectIdName=Id&sObjectClass=" + Account.class.getName())
                .withBody(accountId)
                .request(Account.class);
        return account;
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
    public Map<String, Object> getCdcEvents() {
        Map map = consumerTemplate.receiveBody("seda:events", 10000, Map.class);
        return map;
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

    @Path("sobjects/force-limit")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GlobalObjectsAndHeaders getSObjectsWithForceLimitInfo() {
        // Testing producer with headers
        Exchange exchange = template.to("salesforce:getGlobalObjects")
                .withHeader("Sforce-Limit-Info", Collections.singletonList("api-usage")).send();
        GlobalObjectsAndHeaders objectsAndHeaders = new GlobalObjectsAndHeaders(
                exchange.getMessage().getBody(GlobalObjects.class))
                        .withHeader("Sforce-Limit-Info", exchange.getMessage().getHeader("Sforce-Limit-Info", String.class));
        return objectsAndHeaders;
    }

    @Path("versions")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Version> getListVersions() {
        return template.to("salesforce:getVersions").request(List.class);
    }

    @Path("resources")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public RestResources getListResources() {
        return template.to("salesforce:getResources").request(RestResources.class);
    }

    @Path("basic-info/account")
    @GET
    public SObjectBasicInfo getAccountBasicInfo() {
        return template.to("salesforce:getBasicInfo?sObjectName=Account")
                .request(SObjectBasicInfo.class);
    }

    @Path("describe/account")
    @GET
    public SObjectDescription getAccountDescription() {
        return template.to("salesforce:getDescription?sObjectName=Account")
                .request(SObjectDescription.class);
    }

    @Path("limits")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Limits getLimits() {
        return template.to("salesforce:limits").request(Limits.class);
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
}
