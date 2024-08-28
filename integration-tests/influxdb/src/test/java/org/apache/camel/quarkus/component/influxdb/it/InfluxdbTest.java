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
package org.apache.camel.quarkus.component.influxdb.it;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.component.influxdb.InfluxDbConstants;
import org.apache.camel.component.influxdb.converters.CamelInfluxDbConverters;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
@QuarkusTestResource(InfluxdbTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InfluxdbTest {

    @Test
    @Order(1)
    public void pingTest() {
        RestAssured.given().get("/influxdb/ping").then().body(is(InfluxdbTestResource.INFLUXDB_VERSION));
    }

    @Test
    @Order(2)
    public void insertTest() {
        Point point = createBatchPoints().getPoints().get(0);
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(point)
                .post("/influxdb/insert")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @Test
    @Order(3)
    public void batchInsertTest() {
        Points points = createBatchPoints();
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(points)
                .post("/influxdb/batch")
                .then()
                .statusCode(200)
                .body(is("2"));
    }

    @Test
    @Order(4)
    public void queryTest() {
        // result should contain only 1 result with name 'cpu', because 'cpu' is only part of batchInsert, which was executed before
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("select * from cpu")
                .post("/influxdb/query")
                .then()
                .statusCode(200)
                .body(is("cpu"));
    }

    @Test
    @Order(5)
    public void doesNotAddCamelHeaders() {
        Map<String, Object> pointInMapFormat = new HashMap<>();
        pointInMapFormat.put(InfluxDbConstants.MEASUREMENT_NAME, "testCPU");
        pointInMapFormat.put("busy", 99.999999d);

        org.influxdb.dto.Point p = CamelInfluxDbConverters.fromMapToPoint(pointInMapFormat);
        assertNotNull(p);

        String line = p.lineProtocol();

        assertNotNull(line);
        assertFalse(line.contains(InfluxDbConstants.MEASUREMENT_NAME));
    }

    private static Points createBatchPoints() {
        Points points = new Points();
        points.setDatabase(InfluxdbResource.DB_NAME);

        Point point1 = new Point();
        point1.setMeasurement("disk");
        point1.setTime(System.currentTimeMillis());
        point1.addField("idle", 90L);
        point1.addField("user", 9L);
        point1.addField("system", 1L);

        Point point2 = new Point();
        point2.setMeasurement("cpu");
        point2.setTime(System.currentTimeMillis());
        point2.addField("used", 8L);
        point2.addField("free", 1L);

        points.addPoint(point1);
        points.addPoint(point2);

        return points;
    }
}
