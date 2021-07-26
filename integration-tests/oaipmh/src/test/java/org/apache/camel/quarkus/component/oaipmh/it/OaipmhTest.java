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
package org.apache.camel.quarkus.component.oaipmh.it;

import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(OaipmhTestResource.class)
class OaipmhTest {

    //@Test
    void consumerListRecordsShouldReturn532Records() {
        await().atMost(10, TimeUnit.SECONDS).until(() -> {
            String[] records = get("/oaipmh/consumerListRecords").then().statusCode(200).extract().as(String[].class);
            return records.length == 532;
        });
    }

    //@Test
    void consumerListRecordsParticularCaseShouldReturn45Records() {
        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            String path = "/oaipmh/consumerListRecordsParticularCase";
            String[] records = get(path).then().statusCode(200).extract().as(String[].class);
            return records.length == 45;
        });
    }

    //@Test
    void consumerIdentifyHttpsShouldReturnSingleRecord() {
        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            String[] records = get("/oaipmh/consumerIdentifyHttps").then().statusCode(200).extract().as(String[].class);
            return records.length == 1;
        });
    }

    //@Test
    void producerListRecordsShouldReturn532Records() {
        get("/oaipmh/producerListRecords").then().statusCode(200).body("size()", is(532));
    }

    //@Test
    void producerGetRecordShouldReturnSingleRecord() {
        String id = "oai:dspace.ucuenca.edu.ec:123456789/32374";
        given().body(id).get("/oaipmh/producerGetRecord").then().statusCode(200).body("size()", is(1));
    }

}
