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
package org.apache.camel.quarkus.component.spark.it;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import static org.hamcrest.core.Is.is;

@QuarkusTest
class SparkTest {

    //@Test
    public void rddCount() throws IOException {
        int lineCount = 0;
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(getClass().getClassLoader().getResourceAsStream("testrdd.txt")))) {
            while (r.readLine() != null) {
                lineCount++;
            }
        }

        RestAssured.get("/spark/rdd/count")
                .then()
                .statusCode(200)
                .body(is(String.valueOf(lineCount)));
    }

    //@Test
    // TODO: Spark 2.x does not support Java 9+ https://github.com/apache/camel-quarkus/issues/1955
    @EnabledForJreRange(max = JRE.JAVA_8)
    public void conditionalDataframe() throws IOException {
        RestAssured.get("/spark/dataframe/Micra/count")
                .then()
                .statusCode(200)
                .body(is(String.valueOf(1)));
    }

    //@Test
    @Disabled // TODO this does not work on plain Camel either https://github.com/apache/camel-quarkus/issues/1956
    public void hiveCount() throws IOException {
        RestAssured.get("/spark/hive/count")
                .then()
                .statusCode(200)
                .body(is(String.valueOf(2)));
    }

}
