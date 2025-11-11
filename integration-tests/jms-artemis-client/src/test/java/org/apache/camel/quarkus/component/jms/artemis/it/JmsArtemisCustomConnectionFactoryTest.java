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
package org.apache.camel.quarkus.component.jms.artemis.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.camel.quarkus.messaging.jms.AbstractJmsMessagingTest;
import org.apache.camel.quarkus.test.support.activemq.ActiveMQTestResource;
import org.junit.jupiter.api.Test;

import static org.apache.camel.quarkus.component.jms.artemis.it.CustomConnectionFactory.RECONNECT_ATTEMPTS;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestProfile(JmsArtemisCustomConnectionFactory.class)
@QuarkusTestResource(initArgs = {
        @ResourceArg(name = "modules", value = "quarkus.artemis"),
        @ResourceArg(name = "java-args", value = "-Dbrokerconfig.securityEnabled=false")
}, value = ActiveMQTestResource.class)
public class JmsArtemisCustomConnectionFactoryTest extends AbstractJmsMessagingTest {
    @Test
    public void connectionFactoryImplementation() {
        RestAssured.get("/messaging/jms/artemis/connection/factory")
                .then()
                .statusCode(200)
                .body(is(ActiveMQConnectionFactory.class.getName()));
    }

    @Test
    public void connectionFactoryReconnectAttemptsConfigured() {
        RestAssured.get("/messaging/jms/artemis/connection/factory/reconnectAttempts")
                .then()
                .statusCode(200)
                .body(is(String.valueOf(RECONNECT_ATTEMPTS)));
    }

}
