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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.camel.quarkus.test.support.aws2.Aws2TestEnvContext;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestEnvCustomizer;
import org.apache.commons.lang3.RandomStringUtils;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.StreamSpecification;
import software.amazon.awssdk.services.dynamodb.model.StreamViewType;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;

public class Aws2DdbTestEnvCustomizer implements Aws2TestEnvCustomizer {

    @Override
    public Service[] localstackServices() {
        return new Service[] { Service.DYNAMODB, Service.DYNAMODB_STREAMS };
    }

    @Override
    public void customize(Aws2TestEnvContext envContext) {

        final String tableName = "camel-quarkus-" + RandomStringUtils.randomAlphanumeric(16).toLowerCase(Locale.ROOT);
        envContext.property("aws-ddb.table-name", tableName);

        final String tableNameOperations = "camel-quarkus-operations-"
                + RandomStringUtils.randomAlphanumeric(16).toLowerCase(Locale.ROOT);
        envContext.property("aws-ddb.operations-table-name", tableNameOperations);

        final String tableNameStreams = "camel-quarkus-streams-"
                + RandomStringUtils.randomAlphanumeric(16).toLowerCase(Locale.ROOT);
        envContext.property("aws-ddb.stream-table-name", tableNameStreams);

        List<String> tableNames = Stream.of(tableName, tableNameStreams, tableNameOperations).collect(Collectors.toList());

        final DynamoDbClient client = envContext.client(Service.DYNAMODB, DynamoDbClient::builder);
        {
            final String keyColumn = "key";

            for (String table : tableNames) {
                client.createTable(
                        createTableRequest(table, keyColumn)
                                .build());
            }

            for (String table : tableNames) {
                try (DynamoDbWaiter dbWaiter = client.waiter()) {
                    dbWaiter.waitUntilTableExists(DescribeTableRequest.builder()
                            .tableName(table)
                            .build());
                }

                envContext.closeable(() -> client.deleteTable(DeleteTableRequest.builder().tableName(table).build()));
            }
        }

        Map<String, String> envContextProperties = envContext.getProperies();
        String accessKey = envContextProperties.get("camel.component.aws2-ddb.access-key");
        String secretKey = envContextProperties.get("camel.component.aws2-ddb.secret-key");
        String region = envContextProperties.get("camel.component.aws2-ddb.region");

        envContext.property("quarkus.dynamodb.aws.credentials.static-provider.access-key-id", accessKey);
        envContext.property("quarkus.dynamodb.aws.credentials.static-provider.secret-access-key", secretKey);
        envContext.property("quarkus.dynamodb.aws.region", region);
        envContext.property("quarkus.dynamodb.aws.credentials.type", "static");

        // Propagate localstack environment config to Quarkus AWS if required
        Optional<String> overrideEndpoint = envContextProperties
                .keySet()
                .stream()
                .filter(key -> key.endsWith("uri-endpoint-override"))
                .findFirst();

        if (overrideEndpoint.isPresent()) {
            String endpoint = envContextProperties.get(overrideEndpoint.get());
            envContext.property("quarkus.dynamodb.endpoint-override", endpoint);
        }
    }

    private CreateTableRequest.Builder createTableRequest(String tableName, String keyColumn) {
        CreateTableRequest.Builder builder = CreateTableRequest.builder()
                .attributeDefinitions(AttributeDefinition.builder()
                        .attributeName(keyColumn)
                        .attributeType(ScalarAttributeType.S)
                        .build())
                .keySchema(KeySchemaElement.builder()
                        .attributeName(keyColumn)
                        .keyType(KeyType.HASH)
                        .build())
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(10L)
                        .writeCapacityUnits(10L)
                        .build());
        if (tableName.contains("streams")) {
            builder.streamSpecification(StreamSpecification.builder()
                    .streamEnabled(true)
                    .streamViewType(StreamViewType.NEW_AND_OLD_IMAGES)
                    .build());
        }

        return builder.tableName(tableName);
    }
}
