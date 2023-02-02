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
package org.apache.camel.quarkus.component.aws2.ddb.it;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.aws2.ddb.Ddb2Constants;
import org.apache.camel.component.aws2.ddb.Ddb2Operations;
import org.apache.camel.quarkus.test.support.aws2.BaseAws2Resource;
import org.apache.camel.util.CollectionHelper;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughputDescription;

@Path("/aws2-ddb")
@ApplicationScoped
public class Aws2DdbResource extends BaseAws2Resource {

    public Aws2DdbResource() {
        super("ddb");
    }

    public enum Table {
        basic, operations, stream
    }

    @ConfigProperty(name = "aws-ddb.table-name")
    String tableName;

    @ConfigProperty(name = "aws-ddb.operations-table-name")
    String operationsTableName;

    @ConfigProperty(name = "aws-ddb.stream-table-name")
    String streamTableName;

    @Inject
    ProducerTemplate producerTemplate;

    @SuppressWarnings("serial")
    @Path("/item/{key}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response post(String message,
            @PathParam("key") String key,
            @QueryParam("table") String table) throws Exception {
        final Map<String, AttributeValue> item = new HashMap<>() {
            {
                put("key", AttributeValue.builder()
                        .s(key).build());
                put("value", AttributeValue.builder()
                        .s(message).build());
            }
        };
        producerTemplate.sendBodyAndHeaders(
                componentUri(Table.valueOf(table), Ddb2Operations.PutItem),
                message,
                new HashMap<>() {
                    {
                        put(Ddb2Constants.CONSISTENT_READ, true);
                        put(Ddb2Constants.ITEM, item);
                    }
                });
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @SuppressWarnings("unchecked")
    @Path("/item/{key}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getItem(@PathParam("key") String key) {
        final Map<String, AttributeValue> item = (Map<String, AttributeValue>) producerTemplate
                .send(componentUri(Ddb2Operations.GetItem),
                        e -> {
                            e.getMessage().setHeader(Ddb2Constants.CONSISTENT_READ, true);
                            e.getMessage().setHeader(Ddb2Constants.ATTRIBUTE_NAMES,
                                    new HashSet<String>(Arrays.asList("key", "value")));
                            e.getMessage().setHeader(Ddb2Constants.KEY,
                                    Collections.<String, AttributeValue> singletonMap("key",
                                            AttributeValue.builder().s(key).build()));

                        })
                .getMessage()
                .getHeader(Ddb2Constants.ATTRIBUTES, Map.class);
        final AttributeValue val = item.get("value");
        return val == null ? null : val.s();
    }

    @SuppressWarnings("serial")
    @Path("/item/{key}")
    @PUT
    @Produces(MediaType.TEXT_PLAIN)
    public void updateItem(String message, @PathParam("key") String key, @QueryParam("table") String table) throws Exception {
        producerTemplate.sendBodyAndHeaders(
                componentUri(Table.valueOf(table), Ddb2Operations.UpdateItem),
                null,
                new HashMap<>() {
                    {
                        put(
                                Ddb2Constants.KEY,
                                Collections.singletonMap("key", AttributeValue.builder().s(key).build()));
                        put(
                                Ddb2Constants.UPDATE_VALUES,
                                Collections.singletonMap(
                                        "value",
                                        AttributeValueUpdate.builder()
                                                .action(AttributeAction.PUT)
                                                .value(AttributeValue.builder().s(message).build())
                                                .build()));
                    }
                });
    }

    @SuppressWarnings("serial")
    @Path("/item/{key}")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public void deleteItem(@PathParam("key") String key, @QueryParam("table") String table) throws Exception {
        producerTemplate.sendBodyAndHeaders(
                componentUri(Table.valueOf(table), Ddb2Operations.DeleteItem),
                null,
                new HashMap<>() {
                    {
                        put(Ddb2Constants.CONSISTENT_READ, true);
                        put(Ddb2Constants.KEY,
                                Collections.singletonMap("key",
                                        AttributeValue.builder().s(key).build()));
                    }
                });
    }

    @SuppressWarnings("unchecked")
    @Path("/batchItems")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> batchItems(List<String> keyValues) {
        Map<String, AttributeValue>[] keyAttrs = keyValues.stream()
                .map(v -> Collections.singletonMap("key", AttributeValue.builder().s(v).build())).toArray(Map[]::new);
        Map<String, KeysAndAttributes> keysAttrs = Collections.singletonMap(operationsTableName,
                KeysAndAttributes.builder().keys(keyAttrs).build());

        Map<String, List<Map<AttributeValue, AttributeValue>>> result = (Map<String, List<Map<AttributeValue, AttributeValue>>>) producerTemplate
                .send(componentUri(Table.operations, Ddb2Operations.BatchGetItems),
                        e -> e.getIn().setHeader(Ddb2Constants.BATCH_ITEMS, keysAttrs))
                .getMessage().getHeader(Ddb2Constants.BATCH_RESPONSE);

        Map<String, String> collected = new HashMap<>();
        for (Map<AttributeValue, AttributeValue> m : result.get(operationsTableName)) {
            collected.put(m.get("key").s(), m.get("value").s());
        }
        return collected;
    }

    @SuppressWarnings("unchecked")
    @Path("/query")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> query(String keyEq) {
        Map<String, Condition> keyConditions = new HashMap<>();
        Condition.Builder condition = Condition.builder().comparisonOperator(ComparisonOperator.EQ.toString())
                .attributeValueList(AttributeValue.builder().s(keyEq).build());

        keyConditions.put("key", condition.build());

        List<Map<AttributeValue, AttributeValue>> result = (List<Map<AttributeValue, AttributeValue>>) producerTemplate
                .send(componentUri(Table.operations, Ddb2Operations.Query),
                        e -> {
                            e.getIn().setHeader(Ddb2Constants.ATTRIBUTE_NAMES,
                                    Stream.of("key", "value").collect(Collectors.toList()));
                            e.getIn().setHeader(Ddb2Constants.CONSISTENT_READ, true);
                            e.getIn().setHeader(Ddb2Constants.LIMIT, 10);
                            e.getIn().setHeader(Ddb2Constants.SCAN_INDEX_FORWARD, true);
                            e.getIn().setHeader(Ddb2Constants.KEY_CONDITIONS, keyConditions);
                        })
                .getMessage().getHeader(Ddb2Constants.ITEMS);

        Map<String, String> collected = new HashMap<>();
        for (Map<AttributeValue, AttributeValue> m : result) {
            collected.put(m.get("key").s(), m.get("value").s());
        }
        return collected;
    }

    @SuppressWarnings("unchecked")
    @Path("/scan")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> scan() {
        List<Map<AttributeValue, AttributeValue>> result = (List<Map<AttributeValue, AttributeValue>>) producerTemplate
                .send(componentUri(Table.operations, Ddb2Operations.Scan),
                        e -> {
                            e.getIn().setHeader(Ddb2Constants.ATTRIBUTE_NAMES,
                                    Stream.of("key", "value").collect(Collectors.toList()));
                            e.getIn().setHeader(Ddb2Constants.CONSISTENT_READ, true);
                        })
                .getMessage().getHeader(Ddb2Constants.ITEMS);

        Map<String, String> collected = new HashMap<>();
        for (Map<AttributeValue, AttributeValue> m : result) {
            collected.put(m.get("key").s(), m.get("value").s());
        }
        return collected;
    }

    @Path("/updateTable")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateTable(int capacity) throws Exception {
        producerTemplate
                .send(componentUri(Table.operations, Ddb2Operations.UpdateTable),
                        e -> {
                            e.getIn().setHeader(Ddb2Constants.READ_CAPACITY, capacity);
                            e.getIn().setHeader(Ddb2Constants.WRITE_CAPACITY, capacity);
                        });
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @Path("/operation")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> operation(String operation) {
        final Message message = producerTemplate
                .send(componentUri(Table.operations, Ddb2Operations.valueOf(operation)), e -> {
                })
                .getMessage();
        return message.getHeaders().entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> {
                    if (e.getValue() instanceof List) {
                        return ((List) e.getValue()).size();
                    }
                    if (e.getValue() instanceof ProvisionedThroughputDescription) {
                        ProvisionedThroughputDescription ptd = (ProvisionedThroughputDescription) e.getValue();
                        return CollectionHelper.mapOf(Ddb2Constants.READ_CAPACITY, ptd.readCapacityUnits(),
                                Ddb2Constants.WRITE_CAPACITY, ptd.writeCapacityUnits());
                    }
                    if (Ddb2Constants.TABLE_NAME.equals(e.getKey()) && operationsTableName.equals(e.getValue())) {
                        return Table.operations.toString();
                    }
                    return e.getValue() == null ? "" : e.getValue().toString();
                }));
    }

    private String componentUri(Ddb2Operations op) {
        return componentUri(Table.basic, op);
    }

    private String componentUri(Table table, Ddb2Operations op) {
        String tableName;

        switch (table) {
        case operations:
            tableName = this.operationsTableName;
            break;
        case stream:
            tableName = this.streamTableName;
            break;
        default:
            tableName = this.tableName;
        }
        return "aws2-ddb://" + tableName + "?operation=" + op + "&useDefaultCredentialsProvider=" + isUseDefaultCredentials();
    }
}
