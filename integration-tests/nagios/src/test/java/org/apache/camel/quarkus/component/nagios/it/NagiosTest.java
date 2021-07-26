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
package org.apache.camel.quarkus.component.nagios.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.quarkus.component.nagios.it.NagiosTestResource.MockNscaServerInitializerFactory;

@QuarkusTestResource(NagiosTestResource.class)
@QuarkusTest
public class NagiosTest {

    private static final String EXPECTED_NSCA_FRAME_DIGEST = "315d4b1aed2bb2db79d516f7c651b0d1";

    private MockNscaServerInitializerFactory mockNscaServer;

    public MockNscaServerInitializerFactory getMockNscaServer() {
        return mockNscaServer;
    }

    public void setMockNscaServer(MockNscaServerInitializerFactory mockNscaServer) {
        this.mockNscaServer = mockNscaServer;
    }

    //@Test
    public void sendFixedNscaFrameReturnsExpectedDigest() {
        RestAssured.get("/nagios/send").then().statusCode(204);
        mockNscaServer.verifyFrameReceived(EXPECTED_NSCA_FRAME_DIGEST);
    }

}
