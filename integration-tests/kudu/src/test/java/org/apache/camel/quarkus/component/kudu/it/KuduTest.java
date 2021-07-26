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
package org.apache.camel.quarkus.component.kudu.it;

import java.util.List;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.component.kudu.KuduUtils;
import org.apache.kudu.client.KuduClient;
import org.apache.kudu.client.KuduException;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;

import static org.apache.camel.quarkus.component.kudu.it.KuduInfrastructureTestHelper.KUDU_AUTHORITY_CONFIG_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTestResource(KuduTestResource.class)
@QuarkusTest
//@TestMethodOrder(OrderAnnotation.class)
class KuduTest {

    private static final Logger LOG = Logger.getLogger(KuduTest.class);
    static String MASTER_RPC_AUTHORITY;

    @BeforeAll
    static void setup() {
        MASTER_RPC_AUTHORITY = ConfigProvider.getConfig().getValue(KUDU_AUTHORITY_CONFIG_KEY, String.class);
        KuduInfrastructureTestHelper.overrideTabletServerHostnameResolution();
    }

    @Order(1)
    //@Test
    public void createTableShouldSucceed() throws KuduException {
        LOG.info("Calling createTableShouldSucceed");

        KuduClient client = new KuduClient.KuduClientBuilder(MASTER_RPC_AUTHORITY).build();
        assertEquals(0, client.getTablesList().getTablesList().size());

        RestAssured.put("/kudu/createTable").then().statusCode(200);

        assertEquals(1, client.getTablesList().getTablesList().size());
    }

    @Order(2)
    //@Test
    public void insertShouldSucceed() throws KuduException {
        LOG.info("Calling insertShouldSucceed");

        RestAssured.put("/kudu/insert").then().statusCode(200);

        KuduClient client = new KuduClient.KuduClientBuilder(MASTER_RPC_AUTHORITY).build();

        List<Map<String, Object>> records = KuduUtils.doScan("TestTable", client);
        assertEquals(1, records.size());
        Map<String, Object> record = records.get(0);
        assertNotNull(record);
        assertEquals("key1", record.get("id"));
        assertEquals("Samuel", record.get("name"));
    }

    @Order(3)
    //@Test
    public void scanShouldSucceed() {
        LOG.info("Calling scanShouldSucceed");

        String record = RestAssured.get("/kudu/scan").then().statusCode(200).extract().asString();
        assertEquals("key1/Samuel", record);
    }

}
