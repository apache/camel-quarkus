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

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import static org.hamcrest.Matchers.is;

/**
 * Tests the Pub / Sub API subscriber against {@link MockPubSubService}. When Salesforce credentials are provided,
 * the Pub / Sub API is tested against Salesforce in {@link SalesforceIntegrationTest} instead.
 */
@EnabledIf("org.apache.camel.quarkus.component.salesforce.SalesforceTestResource#isMockBackend")
@QuarkusTest
@QuarkusTestResource(SalesforceTestResource.class)
class SalesforcePubSubTest {

    @Test
    void testPubSubAvro() {
        subscribe("topicSubscribeAvro", "/salesforce/subscribe/event/avro")
                .body(
                        "createdBy", is(MockPubSubService.CREATED_BY),
                        "createdDate", is(MockPubSubService.CREATED_DATE),
                        "testFieldValue", is(MockPubSubService.TEST_FIELD_VALUE));
    }

    @Test
    void testPubSubGenericRecord() {
        subscribe("topicSubscribeGenericRecord", "/salesforce/subscribe/event/generic/record")
                .body(
                        "createdBy", is(MockPubSubService.CREATED_BY),
                        "createdDate", is(String.valueOf(MockPubSubService.CREATED_DATE)),
                        "testFieldValue", is(MockPubSubService.TEST_FIELD_VALUE));
    }

    @Test
    void testPubSubJson() {
        subscribe("topicSubscribeJson", "/salesforce/subscribe/event/json")
                .body(
                        "CreatedById", is(MockPubSubService.CREATED_BY),
                        "CreatedDate", is(MockPubSubService.CREATED_DATE),
                        "Test_Field__c", is(MockPubSubService.TEST_FIELD_VALUE));
    }

    @Test
    void testPubSubPojo() {
        subscribe("topicSubscribePojo", "/salesforce/subscribe/event/pojo")
                .body(
                        "createdBy", is(MockPubSubService.CREATED_BY),
                        "createdDate", is(MockPubSubService.CREATED_DATE),
                        "testFieldValue", is(MockPubSubService.TEST_FIELD_VALUE));
    }

    private static ValidatableResponse subscribe(String routeId, String path) {
        try {
            RestAssured.post("/salesforce/route/" + routeId + "/start")
                    .then()
                    .statusCode(200);

            return RestAssured.get(path)
                    .then()
                    .statusCode(200);
        } finally {
            RestAssured.post("/salesforce/route/" + routeId + "/stop")
                    .then()
                    .statusCode(200);
        }
    }
}
