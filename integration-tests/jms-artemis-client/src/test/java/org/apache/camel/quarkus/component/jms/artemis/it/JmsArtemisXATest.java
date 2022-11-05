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

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.core.Is.is;

@QuarkusTest
@TestProfile(JmsArtemisXAEnabled.class)
public class JmsArtemisXATest {
    @Test
    public void testJmsXACommit() {
        RestAssured.given()
                .body("commit")
                .post("/messaging/jms/artemis/xa")
                .then()
                .statusCode(200)
                .body(is("commit"));
    }

    @Test
    public void testJmsXARollback() {
        RestAssured.given()
                .body("fail")
                .post("/messaging/jms/artemis/xa")
                .then()
                .statusCode(200)
                .body(is("rollback"));
    }
}
