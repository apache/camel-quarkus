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
package org.apache.camel.quarkus.component.leveldb.it;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.apache.camel.Exchange;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class LeveldbTest {

    @Test
    public void testAggregate() {
        Map<String, List<Map<String, Object>>> data = testAggregate(LeveldbRouteBuilder.DIRECT_START,
                Arrays.asList("S", "H", "E", "L", "D", "O", "N"));

        List<Map<String, Object>> resultData = data.get(LeveldbRouteBuilder.MOCK_RESULT);

        assertEquals("direct://start", resultData.get(0).get(LeveldbResource.PARAMETER_FROM_ENDPOINT));
    }

    @Test
    public void testAggregateRecovery() {
        Map<String, List<Map<String, Object>>> data = testAggregate(LeveldbRouteBuilder.DIRECT_START_WITH_FAILURE,
                Arrays.asList("S", "H", "E", "L", "D", "O", "N"));

        List<Map<String, Object>> resultData = data.get(LeveldbRouteBuilder.MOCK_RESULT);

        assertEquals(Boolean.TRUE, resultData.get(0).get(Exchange.REDELIVERED));
        assertEquals(2, resultData.get(0).get(Exchange.REDELIVERY_COUNTER));
        assertEquals("direct://startWithFailure", resultData.get(0).get(LeveldbResource.PARAMETER_FROM_ENDPOINT));
    }

    @Test
    public void testDeadLetter() {
        Map<String, List<Map<String, Object>>> data = testAggregate(LeveldbRouteBuilder.DIRECT_START_DEAD_LETTER,
                Arrays.asList("S", "H", "E", "L", "D", "O", "N"),
                LeveldbRouteBuilder.MOCK_DEAD + "," + LeveldbRouteBuilder.MOCK_RESULT + ","
                        + LeveldbRouteBuilder.MOCK_AGGREGATED);

        List<Map<String, Object>> deadData = data.get(LeveldbRouteBuilder.MOCK_DEAD);
        List<Map<String, Object>> resultData = data.get(LeveldbRouteBuilder.MOCK_RESULT);
        List<Map<String, Object>> agreggatedData = data.get(LeveldbRouteBuilder.MOCK_AGGREGATED);

        assertTrue(resultData.isEmpty());

        assertFalse(agreggatedData.get(0).containsKey(Exchange.REDELIVERED));
        assertEquals(Boolean.TRUE, agreggatedData.get(1).containsKey(Exchange.REDELIVERED));
        assertEquals(1, agreggatedData.get(1).get(Exchange.REDELIVERY_COUNTER));
        assertEquals(3, agreggatedData.get(1).get(Exchange.REDELIVERY_MAX_COUNTER));
        assertEquals(Boolean.TRUE, agreggatedData.get(2).containsKey(Exchange.REDELIVERED));
        assertEquals(2, agreggatedData.get(2).get(Exchange.REDELIVERY_COUNTER));
        assertEquals(3, agreggatedData.get(2).get(Exchange.REDELIVERY_MAX_COUNTER));
        assertEquals(Boolean.TRUE, agreggatedData.get(3).containsKey(Exchange.REDELIVERED));
        assertEquals(3, agreggatedData.get(3).get(Exchange.REDELIVERY_COUNTER));
        assertEquals(3, agreggatedData.get(3).get(Exchange.REDELIVERY_MAX_COUNTER));

        assertEquals(Boolean.TRUE, deadData.get(0).containsKey(Exchange.REDELIVERED));
        assertEquals(3, deadData.get(0).get(Exchange.REDELIVERY_COUNTER));
        assertFalse(deadData.get(0).containsKey(Exchange.REDELIVERY_MAX_COUNTER));
    }

    @Test
    public void testBinaryData() {

        boolean theSame = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Arrays.asList("ab", "Sheldon", "cde"))
                .post("/leveldb/aggregateBinary")
                .then()
                .statusCode(201)
                .extract().as(Boolean.class);

        assertTrue(theSame);
    }

    private Map<String, List<Map<String, Object>>> testAggregate(String path, List<String> messages) {
        return testAggregate(path, messages, null);
    }

    private Map<String, List<Map<String, Object>>> testAggregate(String path, List<String> messages, String mocks) {
        RequestSpecification rs = RestAssured.given()
                .queryParam("path", path);

        if (mocks != null) {
            rs = rs.queryParam("mocks", mocks);
        }

        return (Map<String, List<Map<String, Object>>>) rs.contentType(ContentType.JSON)
                .body(messages)
                .post("/leveldb/aggregate")
                .then()
                .statusCode(201)
                .extract().as(Map.class);
    }

    @AfterAll
    public static void afterAll() throws Exception {
        File data = new File(LeveldbRouteBuilder.DATA_FOLDER);
        FileUtils.deleteDirectory(data);
    }

    static byte[] readBytes(InputStream is) throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
        return os.toByteArray();
    }

}
