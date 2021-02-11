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

import java.util.Locale;

import org.apache.camel.quarkus.test.support.aws2.Aws2TestEnvContext;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestEnvCustomizer;
import org.apache.commons.lang3.RandomStringUtils;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;

public class Aws2DdbTestEnvCustomizer implements Aws2TestEnvCustomizer {

    @Override
    public Service[] localstackServices() {
        return new Service[] { Service.DYNAMODB };
    }

    @Override
    public void customize(Aws2TestEnvContext envContext) {

        final String tableName = "camel-quarkus-" + RandomStringUtils.randomAlphanumeric(16).toLowerCase(Locale.ROOT);
        envContext.property("aws-ddb.table-name", tableName);

        final DynamoDbClient client = envContext.client(Service.DYNAMODB, DynamoDbClient::builder);
        {
            final String keyColumn = "key";
            CreateTableResponse tbl = client.createTable(
                    CreateTableRequest.builder()
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
                                    .build())
                            .tableName(tableName)
                            .build());

            try (DynamoDbWaiter dbWaiter = client.waiter()) {
                dbWaiter.waitUntilTableExists(DescribeTableRequest.builder()
                        .tableName(tableName)
                        .build());
            }

            envContext.closeable(() -> client.deleteTable(DeleteTableRequest.builder().tableName(tableName).build()));
        }

    }
}
