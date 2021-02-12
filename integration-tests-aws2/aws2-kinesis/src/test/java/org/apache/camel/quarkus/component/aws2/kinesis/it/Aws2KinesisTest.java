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
package org.apache.camel.quarkus.component.aws2.kinesis.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestResource;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(Aws2TestResource.class)
class Aws2KinesisTest {

    @Test
    public void test() {
        final String msg = "kinesis-" + java.util.UUID.randomUUID().toString().replace("-", "");
        RestAssured.given() //
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/aws2-kinesis/send") //
                .then()
                .statusCode(201);

        RestAssured.get("/aws2-kinesis/receive")
                .then()
                .statusCode(200)
                .body(Matchers.is(msg));
    }

}
