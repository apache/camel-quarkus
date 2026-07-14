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
package org.apache.camel.quarkus.component.quartz.it;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestProfile(QuartzQuarkusSchedulerAutowiredWithSchedulerBeanTest.Profile.class)
class QuartzQuarkusSchedulerAutowiredWithSchedulerBeanTest {

    @Test
    void testQuarkusSchedulerAutowired() {
        RestAssured.get("/quartz/scheduler-name")
                .then()
                .statusCode(200)
                .body(is("QuarkusQuartzScheduler"));

        RestAssured.get("/quartz/scheduler-bean/fired")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    public static class Profile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "quartz-scheduler-bean";
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("camel.component.quartzFromProperties.propertiesFile", "");
        }
    }
}
