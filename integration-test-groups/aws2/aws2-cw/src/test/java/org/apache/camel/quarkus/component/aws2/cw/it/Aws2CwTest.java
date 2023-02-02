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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.component.aws2.cw.Cw2Constants;
import org.apache.camel.quarkus.test.support.aws2.Aws2Client;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestResource;
import org.apache.camel.quarkus.test.support.aws2.BaseAWs2TestSupport;
import org.apache.camel.util.CollectionHelper;
import org.awaitility.Awaitility;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Datapoint;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsRequest;
import software.amazon.awssdk.services.cloudwatch.model.Statistic;

import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(Aws2TestResource.class)
class Aws2CwTest extends BaseAWs2TestSupport {
    private static final Logger LOG = Logger.getLogger(Aws2CwTest.class);

    @Aws2Client(Service.CLOUDWATCH)
    CloudWatchClient client;

    public Aws2CwTest() {
        super("/aws2-cw");
    }

    @Override
    public void testMethodForDefaultCredentialsProvider() {
        final String namespace = "cq-metrics-" + java.util.UUID.randomUUID().toString().replace("-", "");
        final String metricName = "metricName" + java.util.UUID.randomUUID().toString().replace("-", "");

        Map<String, String> item = CollectionHelper.mapOf(
                Cw2Constants.METRIC_NAMESPACE, namespace,
                Cw2Constants.METRIC_NAME, metricName,
                Cw2Constants.METRIC_VALUE, 0,
                Cw2Constants.METRIC_UNIT, "Count",
                Cw2Constants.METRIC_DIMENSION_NAME, "type",
                Cw2Constants.METRIC_DIMENSION_VALUE, "even");

        RestAssured.given()
                .contentType("application/x-www-form-urlencoded; charset=utf-8")
                .formParams(item)
                .post("/aws2-cw/send-metric-map/" + namespace)
                .then()
                .statusCode(201);
    }

    @Test
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

    @Test
    public void headers() {
        final Instant startTime = Instant.ofEpochMilli(System.currentTimeMillis() - 10000);

        final String namespace = "cq-metrics-" + java.util.UUID.randomUUID().toString().replace("-", "");
        final String metricName = "metricName" + java.util.UUID.randomUUID().toString().replace("-", "");
        final int value = (int) (Math.random() * 10000);

        List<Map<String, String>> data = new LinkedList<>();

        data.add(CollectionHelper.mapOf(
                Cw2Constants.METRIC_NAMESPACE, namespace,
                Cw2Constants.METRIC_NAME, metricName,
                Cw2Constants.METRIC_VALUE, 2 * value,
                Cw2Constants.METRIC_UNIT, "Count",
                Cw2Constants.METRIC_DIMENSION_NAME, "type",
                Cw2Constants.METRIC_DIMENSION_VALUE, "even"));
        data.add(CollectionHelper.mapOf(
                Cw2Constants.METRIC_NAMESPACE, namespace,
                Cw2Constants.METRIC_NAME, metricName,
                Cw2Constants.METRIC_VALUE, 2 * value + 2,
                Cw2Constants.METRIC_UNIT, "Count",
                Cw2Constants.METRIC_DIMENSIONS, "type=even"));
        data.add(CollectionHelper.mapOf(
                Cw2Constants.METRIC_NAMESPACE, namespace,
                Cw2Constants.METRIC_NAME, metricName,
                Cw2Constants.METRIC_VALUE, 2 * value + 4,
                Cw2Constants.METRIC_UNIT, "Count",
                Cw2Constants.METRIC_DIMENSION_NAME, "type",
                Cw2Constants.METRIC_DIMENSION_VALUE, "even"));

        data.add(CollectionHelper.mapOf(
                Cw2Constants.METRIC_NAMESPACE, namespace,
                Cw2Constants.METRIC_NAME, metricName,
                Cw2Constants.METRIC_VALUE, 2 * value + 1,
                Cw2Constants.METRIC_UNIT, "Count",
                Cw2Constants.METRIC_DIMENSION_NAME, "type",
                Cw2Constants.METRIC_DIMENSION_VALUE, "odd"));
        data.add(CollectionHelper.mapOf(
                Cw2Constants.METRIC_NAMESPACE, namespace, Cw2Constants.METRIC_NAME, metricName,
                Cw2Constants.METRIC_VALUE, 2 * value + 3,
                Cw2Constants.METRIC_UNIT, "Count",
                Cw2Constants.METRIC_DIMENSION_NAME, "type",
                Cw2Constants.METRIC_DIMENSION_VALUE, "odd"));
        // ignored because of timestamp
        data.add(CollectionHelper.mapOf(
                Cw2Constants.METRIC_NAMESPACE, namespace, Cw2Constants.METRIC_NAME, metricName,
                Cw2Constants.METRIC_VALUE, 2 * value + 5,
                Cw2Constants.METRIC_TIMESTAMP, System.currentTimeMillis() - 24 * 60 * 60 * 1000,
                Cw2Constants.METRIC_UNIT, "Count",
                Cw2Constants.METRIC_DIMENSION_NAME, "type",
                Cw2Constants.METRIC_DIMENSION_VALUE, "odd"));

        for (Map<String, String> item : data) {
            RestAssured.given()
                    .contentType("application/x-www-form-urlencoded; charset=utf-8")
                    .formParams(item)
                    .post("/aws2-cw/send-metric-map/" + namespace)
                    .then()
                    .statusCode(201);
        }

        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(120, TimeUnit.SECONDS).until(
                () -> {

                    List<Datapoint> oddDatapoints = client.getMetricStatistics(
                            GetMetricStatisticsRequest.builder()
                                    .namespace(namespace)
                                    .metricName(metricName)
                                    .statistics(Statistic.SAMPLE_COUNT, Statistic.MINIMUM, Statistic.MAXIMUM)
                                    .startTime(startTime)
                                    .dimensions(Dimension.builder().name("type").value("odd").build())
                                    .endTime(Instant.ofEpochMilli(System.currentTimeMillis() + 10000))
                                    .period(30)
                                    .build())
                            .datapoints();

                    List<Datapoint> evenDatapoints = client.getMetricStatistics(
                            GetMetricStatisticsRequest.builder()
                                    .namespace(namespace)
                                    .metricName(metricName)
                                    .statistics(Statistic.SAMPLE_COUNT, Statistic.AVERAGE)
                                    .startTime(startTime)
                                    .dimensions(Dimension.builder().name("type").value("even").build())
                                    .endTime(Instant.ofEpochMilli(System.currentTimeMillis() + 10000))
                                    .period(30)
                                    .build())
                            .datapoints();
                    LOG.debug("Expecting some datapoints for metric " + namespace + "/" + metricName + " (type='odd'), got "
                            + oddDatapoints);
                    if (oddDatapoints.isEmpty()) {
                        return false;
                    }
                    LOG.debug("Expecting some datapoints for metric " + namespace + "/" + metricName + " (type='even'), got "
                            + evenDatapoints);
                    if (evenDatapoints.isEmpty()) {
                        return false;
                    }

                    Datapoint oddDp = oddDatapoints.get(0);
                    Datapoint evenDp = evenDatapoints.get(0);

                    if (!(oddDp.sampleCount().intValue() == 2
                            && oddDp.minimum().intValue() == 2 * value + 1
                            && oddDp.maximum().intValue() == 2 * value + 3)) {
                        throw new RuntimeException("Unexpected odd datapoint " + oddDp
                                + "; expected sampleCount == 2 && minimum ~ " + (2 * value + 1)
                                + " && maximum ~ " + (2 * value + 3));
                    }

                    if (!(evenDp.average() == 2 * value + 2
                            && evenDp.sampleCount() == 3)) {
                        throw new RuntimeException("Unexpected even datapoint " + evenDp
                                + "; expected sampleCount == 3 && average == " + (2 * value + 2));
                    }

                    return true;
                });

    }

    @Test
    public void customClient() {
        final String namespace = "cq-metrics-" + java.util.UUID.randomUUID().toString().replace("-", "");
        final String metricName = "metricName" + java.util.UUID.randomUUID().toString().replace("-", "");
        final int value = (int) (Math.random() * 10000);

        Map<String, Object> data = CollectionHelper.mapOf(
                Cw2Constants.METRIC_NAMESPACE, namespace,
                Cw2Constants.METRIC_NAME, metricName,
                Cw2Constants.METRIC_VALUE, 2 * value,
                Cw2Constants.METRIC_UNIT, "Count");

        RestAssured.given()
                .contentType("application/x-www-form-urlencoded; charset=utf-8")
                .header("customClientName", "customClient")
                .header("returnExceptionMessage", true)
                .formParams(data)
                .post("/aws2-cw/send-metric-map/" + namespace)
                .then()
                .statusCode(200)
                .body(is("CloudWatchClientMock"));
    }

}
