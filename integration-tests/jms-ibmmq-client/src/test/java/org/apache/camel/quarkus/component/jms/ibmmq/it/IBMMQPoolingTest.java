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

import java.lang.reflect.Method;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import org.apache.camel.quarkus.component.jms.ibmmq.support.IBMMQDestinations;
import org.apache.camel.quarkus.component.jms.ibmmq.support.IBMMQTestResource;
import org.apache.camel.quarkus.messaging.jms.AbstractJmsMessagingTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.hamcrest.Matchers.startsWith;

@QuarkusTest
@QuarkusTestResource(IBMMQTestResource.class)
@EnabledIfSystemProperty(named = "ibm.mq.container.license", matches = "accept")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestProfile(JmsPoolingEnabled.class)
public class IBMMQPoolingTest extends AbstractJmsMessagingTest {
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
    @Override
    public void startRoutes(TestInfo test) {
        for (Method method : test.getTestClass().get().getMethods()) {
            destinations.createQueue(method.getName());
            // Some tests use two queues
            destinations.createQueue(method.getName() + "2");
            destinations.createTopic(method.getName());
        }

        super.startRoutes(test);
    }

    @Test
    public void connectionFactoryImplementation() {
        RestAssured.get("/messaging/jms/ibmmq/connection/factory")
                .then()
                .statusCode(200)
                .body(startsWith("org.apache.camel.quarkus.component.jms.ibmmq.it.IBMMQConnectionFactory"));
    }

    @Override
    public void testJmsTopic() {
        // Ignore testJmsTopic since it can't use setClientId in a pool connection.
    }
}
