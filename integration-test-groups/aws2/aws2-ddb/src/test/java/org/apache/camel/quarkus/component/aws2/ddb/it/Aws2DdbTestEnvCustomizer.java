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

        //copy local properties for the quarkus client
        copyEnvPropertyAs(envContext, "camel.component.aws2-ddb.access-key",
                "AWS_ACCESS_KEY");
        copyEnvPropertyAs(envContext, "camel.component.aws2-ddb.secret-key",
                "AWS_SECRET_KEY");
        copyEnvPropertyAs(envContext, "camel.component.aws2-ddb.uri-endpoint-override",
                "AWS_CONTAINER_CREDENTIALS_FULL_URI");
    }

    private void copyEnvPropertyAs(Aws2TestEnvContext envContext, String oldKey, String newKey) {
        String value = envContext.getProperies().get(oldKey);
        if (value != null) {
            envContext.property(newKey, value);
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
                        .readCapacityUnits(new Long(10))
                        .writeCapacityUnits(new Long(10))
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
