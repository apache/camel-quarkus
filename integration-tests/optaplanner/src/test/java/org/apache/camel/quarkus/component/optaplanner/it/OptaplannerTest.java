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
package org.apache.camel.quarkus.component.optaplanner.it;

import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.quarkus.component.optaplanner.it.domain.TimeTable;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class OptaplannerTest {

    @Test
    public void solveSync() {
        RestAssured.given()
                .get("/optaplanner/solveSync")
                .then()
                .statusCode(200)
                .body("lessonList[0].timeslot", notNullValue(null))
                .body("lessonList[0].room", notNullValue(null));
    }

    @Test
    public void solveASyncWithConsumer() {
        // solve async
        RestAssured.given()
                .get("/optaplanner/solveAsync")
                .then()
                .statusCode(200)
                .body("lessonList[0].timeslot", notNullValue(null))
                .body("lessonList[0].room", notNullValue(null));

        await().atMost(10L, TimeUnit.SECONDS).until(() -> {
            TimeTable result = RestAssured.get("/optaplanner/newBestSolution").then().extract().body().as(TimeTable.class);
            return result != null && result.getLessonList().get(0).getTimeslot() != null
                    && result.getLessonList().get(0).getRoom() != null;
        });
    }

}
