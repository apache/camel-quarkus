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
package org.apache.camel.quarkus.component.splunk.it;

import java.io.IOException;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import org.apache.camel.component.splunk.ProducerType;
import org.apache.camel.quarkus.test.DisabledOnArm;
import org.apache.camel.quarkus.test.support.certificate.TestCertificates;
import org.apache.camel.quarkus.test.support.splunk.SplunkConstants;
import org.apache.camel.quarkus.test.support.splunk.SplunkTestResource;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.NoHttpResponseException;
import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

@TestCertificates(docker = true, certificates = {
        @Certificate(name = "splunk", formats = { Format.PEM, Format.PKCS12 }, password = "password"),
})
@QuarkusTest
@QuarkusTestResource(value = SplunkTestResource.class, initArgs = {
        @ResourceArg(name = "certName", value = "splunk") })
@DisabledOnArm
public class SplunkTest {

    private final static int TIMEOUT_IN_SECONDS = 180;

    @Test
    public void testNormalSearchWithSubmitWithRawData() throws InterruptedException {
        String suffix = "_normalSearchOfSubmit";
        String restUrl = "/splunk/ssl/results/normalSearch";

        write(suffix, ProducerType.SUBMIT, 0, true);

        Awaitility.await().pollInterval(1000, TimeUnit.MILLISECONDS).atMost(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS).until(
                () -> {

                    String result = RestAssured.given()
                            .contentType(ContentType.TEXT)
                            .post(restUrl)
                            .then()
                            .statusCode(200)
                            .extract().asString();

                    return result.contains("Name: Sheldon" + suffix)
                            && result.contains("Name: Leonard" + suffix)
                            && result.contains("Name: Irma" + suffix);
                });
    }

    @Test
    public void testSavedSearchWithTcp() throws InterruptedException {
        testSavedSearchWithTcp(true);
    }

    @Test
    public void testStreamForRealtime() throws InterruptedException, ExecutionException {
        String suffix = "_RealtimeSearchOfStream";
        String restUrl = "/splunk/ssl/results/realtimeSearch";
        //there is a buffer for stream writing, therefore about 1MB of data has to be written into Splunk

        //data are written in separated thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        //execute component server to wait for the result
        Future<Void> futureResult = executor.submit(
                () -> {
                    for (int i = 0; i < 5000; i++) {
                        write(suffix + i, ProducerType.STREAM, 100, false);
                    }
                    return null;
                });

        try {
            Awaitility.await().pollInterval(1000, TimeUnit.MILLISECONDS).atMost(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS).until(
                    () -> {

                        String result = RestAssured.given()
                                .contentType(ContentType.TEXT)
                                .post(restUrl)
                                .then()
                                .statusCode(200)
                                .extract().asString();

                        return result.contains("Name: Sheldon" + suffix)
                                && result.contains("Name: Leonard" + suffix)
                                && result.contains("Name: Irma" + suffix);
                    });
        } finally {
            futureResult.cancel(true);
        }
    }

    @Test
    public void testSavedSearchWithTcpNoSSL() {
        Exception exception = Assertions.assertThrows(IOException.class,
                () -> testSavedSearchWithTcp(false));
        // in case of Splunk connected remotely, it can behaves differently from localhost loopback (ie. it throws `java.net.SocketException: Connection reset`)
        Assertions.assertTrue(exception instanceof NoHttpResponseException || exception instanceof SocketException);
    }

    void testSavedSearchWithTcp(boolean ssl) throws InterruptedException {
        String suffix = "_SavedSearchOfTcp";
        String urlPrefix = ssl ? "https://" : "http://";
        String restUrl = ssl ? "/splunk/ssl/results/savedSearch" : "/splunk/results/savedSearch";
        //create saved search
        Config config = ConfigProvider.getConfig();
        RestAssured.given()
                .relaxedHTTPSValidation()
                .baseUri(urlPrefix + config.getValue(SplunkConstants.PARAM_REMOTE_HOST, String.class))
                .port(config.getValue(SplunkConstants.PARAM_REMOTE_PORT,
                        Integer.class))
                .contentType(ContentType.JSON)
                .param("name", SplunkResource.SAVED_SEARCH_NAME)
                .param("disabled", "0")
                .param("description", "descriptionText")
                .param("search",
                        "sourcetype=\"TCP\" | rex field=_raw \"Name: (?<name>.*) From: (?<from>.*)\"")
                .post("/services/saved/searches")
                .then()
                .statusCode(anyOf(is(201), is(409)));

        //write data via tcp
        write(suffix, ProducerType.TCP, 0, false);

        //there might by delay in receiving the data
        Awaitility.await().pollInterval(1000, TimeUnit.MILLISECONDS).atMost(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS).until(
                () -> {
                    String result = RestAssured.given()
                            .contentType(ContentType.TEXT)
                            .post(restUrl)
                            .then()
                            .statusCode(200)
                            .extract().asString();

                    return result.contains("Name: Sheldon" + suffix)
                            && result.contains("Name: Leonard" + suffix)
                            && result.contains("Name: Irma" + suffix);
                });
    }

    private void write(String suffix, ProducerType producerType, int lengthOfRandomString, boolean raw) {
        String restUrl = "/splunk/ssl/write/";
        Consumer<Map<String, String>> write = data -> RestAssured.given()
                .contentType(ContentType.JSON)
                .queryParam("index", SplunkTestResource.TEST_INDEX)
                .body(data)
                .post(restUrl + producerType.name())
                .then()
                .statusCode(201)
                .body(Matchers.containsString(expectedResult(data)));

        Map<String, String> data1;
        Map<String, String> data2;
        Map<String, String> data3;
        if (raw) {
            data1 = Map.of("_rawData", "Name: Sheldon" + suffix + " From: Alpha Centauri", "data",
                    RandomStringUtils.secure().nextAlphanumeric(lengthOfRandomString));
            data2 = Map.of("_rawData", "Name: Leonard" + suffix + " From: Earth 2.0", "data",
                    RandomStringUtils.secure().nextAlphanumeric(lengthOfRandomString));
            data3 = Map.of("_rawData", "Name: Irma" + suffix + " From: Earth", "data",
                    RandomStringUtils.secure().nextAlphanumeric(lengthOfRandomString));
        } else {
            data1 = Map.of("entity", "Name: Sheldon" + suffix + " From: Alpha Centauri", "data",
                    RandomStringUtils.secure().nextAlphanumeric(lengthOfRandomString));
            data2 = Map.of("entity", "Name: Leonard" + suffix + " From: Earth 2.0", "data",
                    RandomStringUtils.secure().nextAlphanumeric(lengthOfRandomString));
            data3 = Map.of("entity", "Name: Irma" + suffix + " From: Earth", "data",
                    RandomStringUtils.secure().nextAlphanumeric(lengthOfRandomString));
        }

        write.accept(data1);
        write.accept(data2);
        write.accept(data3);
    }

    private String expectedResult(Map<String, String> data) {
        if (data.containsKey("_rawData")) {
            return data.get("_rawData");
        }
        return data.entrySet().stream()
                .map(e -> e.getKey() + "=\"" + e.getValue() + "\"")
                .collect(Collectors.joining(" "));
    }
}
