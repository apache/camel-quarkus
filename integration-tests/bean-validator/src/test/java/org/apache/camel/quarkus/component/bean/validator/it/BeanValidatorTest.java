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
package org.apache.camel.quarkus.component.bean.validator.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

@QuarkusTest
class BeanValidatorTest {

    @Test
    public void test() {
        //forced optional check
        RestAssured.get("/bean-validator/get/optional/honda/123").then().statusCode(400);
        //forced optional check
        RestAssured.get("/bean-validator/get/optional/honda/DD-AB-123").then().statusCode(200);
        //not-forced optional check
        RestAssured.get("/bean-validator/get/start/honda/123").then().statusCode(200);
        //not-forced optional check
        RestAssured.get("/bean-validator/get/start/honda/DD-AB-12").then().statusCode(200);
        //forced null-check
        RestAssured.get("/bean-validator/get/start/honda").then().statusCode(400);
        //Null-check not in optional group and null is valid for minSize
        RestAssured.get("/bean-validator/get/optional/honda").then().statusCode(200);
    }

}
