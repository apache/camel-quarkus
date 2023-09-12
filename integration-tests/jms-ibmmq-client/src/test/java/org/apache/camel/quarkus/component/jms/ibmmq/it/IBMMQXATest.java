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

package org.apache.camel.quarkus.component.jms.ibmmq.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import org.apache.camel.quarkus.component.jms.ibmmq.support.IBMMQDestinations;
import org.apache.camel.quarkus.component.jms.ibmmq.support.IBMMQTestResource;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.hamcrest.core.Is.is;

@QuarkusTest
@QuarkusTestResource(IBMMQTestResource.class)
@EnabledIfSystemProperty(named = "ibm.mq.container.license", matches = "accept")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestProfile(JmsXAEnabled.class)
public class IBMMQXATest {
    private IBMMQDestinations destinations;

    /**
     * IBM MQ needs to have the destinations created before you can use them.
     * <p>
     * This method is called after the routes start, so the routes will print a warning first that the destinations don't
     * exist, only then they are
     * created using this method
     *
     * @param test test
     */
    @BeforeAll
    public void startRoutes(TestInfo test) {
        destinations.createQueue("xa");

        RestAssured.given()
                // see AbstractMessagingTest#beforeAll
                .port(ConfigProvider.getConfig().getValue("quarkus.http.test-port", Integer.class))
                .get("/messaging/jms/ibmmq/routes/startXA");
    }

    @Test
    public void connectionFactoryImplementation() {
        RestAssured.get("/messaging/jms/ibmmq/connection/factory")
                .then()
                .statusCode(200)
                .body(is("org.messaginghub.pooled.jms.JmsPoolXAConnectionFactory"));
    }

    @Test
    public void testJmsXACommit() {
        RestAssured.given()
                .body("commit")
                .post("/messaging/jms/ibmmq/xa")
                .then()
                .statusCode(200)
                .body(is("commit"));
    }

    @Test
    public void testJmsXARollback() {
        RestAssured.given()
                .body("fail")
                .post("/messaging/jms/ibmmq/xa")
                .then()
                .statusCode(200)
                .body(is("rollback"));
    }
}
