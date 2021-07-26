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
package org.apache.camel.quarkus.component.aws2.cw.it;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.test.support.aws2.Aws2Client;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestResource;
import org.awaitility.Awaitility;
import org.jboss.logging.Logger;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Datapoint;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsRequest;
import software.amazon.awssdk.services.cloudwatch.model.Statistic;

@QuarkusTest
@QuarkusTestResource(Aws2TestResource.class)
class Aws2CwTest {
    private static final Logger LOG = Logger.getLogger(Aws2CwTest.class);

    @Aws2Client(Service.CLOUDWATCH)
    CloudWatchClient client;

    //@Test
    public void metric() {

        final Instant startTime = Instant.ofEpochMilli(System.currentTimeMillis() - 10000);

        final String namespace = "cq-metrics-" + java.util.UUID.randomUUID().toString().replace("-", "");
        final String metricName = "metricName" + java.util.UUID.randomUUID().toString().replace("-", "");
        final int value = (int) (Math.random() * 10000);
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(value)
                .post("/aws2-cw/send-metric/" + namespace + "/" + metricName + "/Count")
                .then()
                .statusCode(201);

        final double precision = 0.0001;
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(120, TimeUnit.SECONDS).until(
                () -> {

                    List<Datapoint> datapoints = client.getMetricStatistics(
                            GetMetricStatisticsRequest.builder()
                                    .namespace(namespace)
                                    .metricName(metricName)
                                    .statistics(Statistic.SAMPLE_COUNT, Statistic.MINIMUM, Statistic.MAXIMUM)
                                    .startTime(startTime)
                                    .endTime(Instant.ofEpochMilli(System.currentTimeMillis() + 10000))
                                    .period(1)
                                    .build())
                            .datapoints();
                    LOG.info("Expecting some datapoints for metric " + namespace + "/" + metricName + ", got " + datapoints);
                    if (datapoints.isEmpty()) {
                        return false;
                    }

                    Datapoint dp = datapoints.get(0);

                    if (dp.sampleCount().doubleValue() + precision > 1.0
                            && Math.abs(dp.minimum().doubleValue() - value) < precision
                            && Math.abs(dp.maximum().doubleValue() - value) < precision) {
                        return true;
                    }
                    throw new RuntimeException("Unexpected datapoint " + dp + "; expected sampleCount ~ 1 && minimum ~ " + value
                            + " && maximum ~ " + value);
                });

    }

}
