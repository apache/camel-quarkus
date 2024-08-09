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
package org.apache.camel.quarkus.component.splunk.hec.it;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.test.support.splunk.SplunkConstants;
import org.apache.camel.quarkus.test.support.splunk.SplunkTestResource;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.shaded.org.hamcrest.core.StringContains;

@QuarkusTest
@WithTestResource(value = SplunkTestResource.class, initArgs = {
        @ResourceArg(name = "localhost_cert", value = "target/certs/localhost.pem"),
        @ResourceArg(name = "ca_cert", value = "target/certs/splunkca.pem"),
        @ResourceArg(name = "localhost_keystore", value = "target/certs/localhost.jks"),
        @ResourceArg(name = "keystore_password", value = "password") })
public class SplunkHecTest {

    @Test
    public void produce() {

        String url = String.format("https://%s:%d",
                getConfigValue(SplunkConstants.PARAM_REMOTE_HOST, String.class),
                getConfigValue(SplunkConstants.PARAM_REMOTE_PORT, Integer.class));

        RestAssured.given()
                .body("Hello Sheldon")
                .post("/splunk-hec/send/sslContextParameters")
                .then()
                .statusCode(200);

        //there might a delay between the data written and received by the search, therefore await()
        Awaitility.await().atMost(30, TimeUnit.SECONDS).until(
                () -> RestAssured.given()
                        .request()
                        .formParam("search", "search index=\"testindex\"")
                        .formParam("exec_mode", "oneshot")
                        .relaxedHTTPSValidation()
                        .auth().basic("admin", "password")
                        .post(url + "/services/search/jobs")
                        .then().statusCode(200)
                        .extract().asString(),
                StringContains.containsString("Hello Sheldon"));
    }

    @Test
    public void produceWithWrongCertificate() {
        RestAssured.given()
                .body("Hello Sheldon")
                .post("/splunk-hec/send/wrongSslContextParameters")
                .then()
                .statusCode(500)
                .body(org.hamcrest.core.StringContains.containsString("signature check failed"));
    }

    @Test
    public void testIndexTime() {
        String url = String.format("https://%s:%d",
                getConfigValue(SplunkConstants.PARAM_REMOTE_HOST, String.class),
                getConfigValue(SplunkConstants.PARAM_REMOTE_PORT, Integer.class));

        //get time one day ago
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -1);

        //send an event with text 'Hello Time 01'
        RestAssured.given()
                .body("Hello time 01")
                .post("/splunk-hec/send/sslContextParameters")
                .then()
                .statusCode(200);

        //send an event with text 'Hello Time 02', with overriden time
        RestAssured.given()
                .body("Hello time 02")
                .queryParam("indexTime", calendar.getTimeInMillis())
                .post("/splunk-hec/send/sslContextParameters")
                .then()
                .statusCode(200);

        //there might a delay between the data written and received by the search, therefore await()
        Awaitility.await().atMost(30, TimeUnit.SECONDS).until(
                () -> {
                    //get time from HelloTime01
                    List<String> times01 = RestAssured.given()
                            .request()
                            .formParam("search", "search body=\"hello time 01\" index=\"testindex\"")
                            .formParam("exec_mode", "oneshot")
                            .formParam("output_mode", "json")
                            .relaxedHTTPSValidation()
                            .auth().basic("admin", "password")
                            .post(url + "/services/search/jobs")
                            .then().statusCode(200)
                            .contentType(ContentType.JSON)
                            .extract().path("results._time"); //get time from HelloTime01
                    List<String> times02 = RestAssured.given()
                            .request()
                            .formParam("search", "search body=\"hello time 02\" index=\"testindex\"")
                            .formParam("exec_mode", "oneshot")
                            .formParam("output_mode", "json")
                            .relaxedHTTPSValidation()
                            .auth().basic("admin", "password")
                            .post(url + "/services/search/jobs")
                            .then().statusCode(200)
                            .contentType(ContentType.JSON)
                            .extract().path("results._time");

                    //time 2 has to be before time 1, event if was created later
                    return times01.size() == 1 && times02.size() == 1 && times02.get(0).compareTo(times01.get(0)) < 1;
                });

    }

    private <T> T getConfigValue(String key, Class<T> type) {
        return ConfigProvider.getConfig().getValue(key, type);
    }
}
