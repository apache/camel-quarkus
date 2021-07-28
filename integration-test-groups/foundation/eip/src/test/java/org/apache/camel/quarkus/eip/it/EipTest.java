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
package org.apache.camel.quarkus.eip.it;

import java.util.Arrays;
import java.util.List;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

@QuarkusTest
class EipTest {

    private static final Logger LOG = Logger.getLogger(EipTest.class);

    @Test
    public void claimCheckByHeader() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Secret")
                .queryParam("claimCheckId", "foo")
                .post("/eip/route/claimCheckByHeader")
                .then()
                .statusCode(200);

        RestAssured.get("/eip/mock/claimCheckByHeader/4/10000/body")
                .then()
                .statusCode(200)
                .body(Matchers.is("Bye World,Secret,Hi World,Secret"));

    }

    @Test
    public void customLoadBalancer() {
        final List<String> messages = Arrays.asList("a", "b", "c", "d");
        for (String msg : messages) {
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .body(msg)
                    .post("/eip/route/customLoadBalancer")
                    .then()
                    .statusCode(200);
        }

        RestAssured.get("/eip/mock/customLoadBalancer1/2/10000/body")
                .then()
                .statusCode(200)
                .body(Matchers.is("a,c"));

        RestAssured.get("/eip/mock/customLoadBalancer2/2/10000/body")
                .then()
                .statusCode(200)
                .body(Matchers.is("b,d"));

    }

    @Test
    public void roundRobinLoadBalancer() {
        final List<String> messages = Arrays.asList("a", "b", "c", "d");
        for (String msg : messages) {
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .body(msg)
                    .post("/eip/route/roundRobinLoadBalancer")
                    .then()
                    .statusCode(200);
        }

        RestAssured.get("/eip/mock/roundRobinLoadBalancer1/2/10000/body")
                .then()
                .statusCode(200)
                .body(Matchers.is("a,c"));

        RestAssured.get("/eip/mock/roundRobinLoadBalancer2/2/10000/body")
                .then()
                .statusCode(200)
                .body(Matchers.is("b,d"));

    }

    @Test
    public void enrich() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Franz")
                .post("/eip/route/enrich")
                .then()
                .statusCode(200)
                .body(Matchers.is("Hello Franz"));

    }

    @Test
    public void failover() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Arthur")
                .post("/eip/route/failover")
                .then()
                .statusCode(200)
                .body(Matchers.is("Hello from failover2 Arthur"));

    }

    @Test
    public void loop() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("foo")
                .post("/eip/route/loop")
                .then()
                .statusCode(200);

        RestAssured.get("/eip/mock/loop/3/5000/body")
                .then()
                .statusCode(200)
                .body(Matchers.is("foo,foo,foo"));

    }

    @Test
    public void multicast() {
        final List<String> messages = Arrays.asList("a", "b", "c", "d");
        for (String msg : messages) {
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .body(msg)
                    .post("/eip/route/multicast")
                    .then()
                    .statusCode(200);
        }

        RestAssured.get("/eip/mock/multicast1/4/5000/body")
                .then()
                .statusCode(200)
                .body(Matchers.is("a,b,c,d"));

        RestAssured.get("/eip/mock/multicast2/4/5000/body")
                .then()
                .statusCode(200)
                .body(Matchers.is("a,b,c,d"));

        RestAssured.get("/eip/mock/multicast3/4/5000/body")
                .then()
                .statusCode(200)
                .body(Matchers.is("a,b,c,d"));

    }

    @Test
    public void recipientList() {
        final List<String> messages = Arrays.asList("a", "b", "c", "d");
        for (String msg : messages) {
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .body(msg)
                    .post("/eip/route/recipientList")
                    .then()
                    .statusCode(200);
        }

        RestAssured.get("/eip/mock/recipientList1/4/5000/body")
                .then()
                .statusCode(200)
                .body(Matchers.is("a,b,c,d"));

        RestAssured.get("/eip/mock/recipientList2/4/5000/body")
                .then()
                .statusCode(200)
                .body(Matchers.is("a,b,c,d"));

        RestAssured.get("/eip/mock/recipientList3/4/5000/body")
                .then()
                .statusCode(200)
                .body(Matchers.is("a,b,c,d"));

    }

    @Test
    public void removeHeader() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("baz")
                .queryParam("headerToKeep", "foo")
                .queryParam("headerToRemove", "bar")
                .post("/eip/route/removeHeader")
                .then()
                .statusCode(200);

        RestAssured.get("/eip/mock/removeHeader/1/5000/header")
                .then()
                .statusCode(200)
                .body(
                        Matchers.allOf(
                                Matchers.containsString("headerToKeep=foo"),
                                Matchers.not(Matchers.containsString("headerToRemove"))));

    }

    @Test
    public void removeHeaders() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("baz")
                .queryParam("headerToKeep", "keepFoo")
                .queryParam("headerToRemove1", "bar1")
                .queryParam("headerToRemove2", "bar2")
                .post("/eip/route/removeHeaders")
                .then()
                .statusCode(200);

        RestAssured.get("/eip/mock/removeHeaders/1/5000/header")
                .then()
                .statusCode(200)
                .body(
                        Matchers.allOf(
                                Matchers.containsString("headerToKeep=keepFoo"),
                                Matchers.not(Matchers.containsString("headerToRemove1")),
                                Matchers.not(Matchers.containsString("headerToRemove2"))));

    }

    @Test
    public void removeProperty() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("baz")
                .queryParam("propertyToKeep", "keep")
                .queryParam("propertyToRemove", "bar")
                .post("/eip/route/removeProperty")
                .then()
                .statusCode(200);

        RestAssured.get("/eip/mock/removeProperty/1/5000/property")
                .then()
                .statusCode(200)
                .body(
                        Matchers.allOf(
                                Matchers.containsString("propertyToKeep=keep"),
                                Matchers.not(Matchers.containsString("propertyToRemove"))));

    }

    @Test
    public void removeProperties() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("baz")
                .queryParam("propertyToKeep", "keepProp")
                .queryParam("propertyToRemove1", "bar1")
                .queryParam("propertyToRemove2", "bar2")
                .post("/eip/route/removeProperties")
                .then()
                .statusCode(200);

        RestAssured.get("/eip/mock/removeProperties/1/5000/property")
                .then()
                .statusCode(200)
                .body(
                        Matchers.allOf(
                                Matchers.containsString("propertyToKeep=keepProp"),
                                Matchers.not(Matchers.containsString("propertyToRemove1")),
                                Matchers.not(Matchers.containsString("propertyToRemove2"))));

    }

    @Test
    public void routingSlip() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("message-1")
                .queryParam("routingSlipHeader", "mock:routingSlip1,mock:routingSlip2")
                .post("/eip/route/routingSlip")
                .then()
                .statusCode(200);

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("message-2")
                .queryParam("routingSlipHeader", "mock:routingSlip2,mock:routingSlip3")
                .post("/eip/route/routingSlip")
                .then()
                .statusCode(200);

        RestAssured.get("/eip/mock/routingSlip1/1/5000/body")
                .then()
                .statusCode(200)
                .body(Matchers.is("message-1"));

        RestAssured.get("/eip/mock/routingSlip2/2/5000/body")
                .then()
                .statusCode(200)
                .body(Matchers.is("message-1,message-2"));

        RestAssured.get("/eip/mock/routingSlip3/1/5000/body")
                .then()
                .statusCode(200)
                .body(Matchers.is("message-2"));

    }

    @Test
    public void sample() {
        final int durationSec = 2;
        LOG.infof("About to sent messages for %d seconds", durationSec);
        final long deadline = System.currentTimeMillis() + (durationSec * 1000); // two seconds ahead
        int i = 0;
        while (System.currentTimeMillis() < deadline) {
            /* Send messages for 2 seconds */
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .body("message-" + i++)
                    .post("/eip/route/sample")
                    .then()
                    .statusCode(200);
        }
        LOG.infof("Sent %d messages", i);
        /*
         * We should normally get just 2 samples in 2 seconds using the default sample rate of 1 message per second
         * But timing is hard in programming, let's allow one more
         */
        int overratedSampleUpperBound = durationSec + 1;
        Assertions.assertThat(i).isGreaterThan(overratedSampleUpperBound);
        String[] samples = RestAssured.get("/eip/mock/sample/1+/5000/body")
                .then()
                .statusCode(200)
                .extract()
                .body().asString().split(",");
        LOG.infof("Got %d samples", samples.length);
        Assertions.assertThat(samples.length).isBetween(1, overratedSampleUpperBound);
    }

    @Test
    public void step() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Monty")
                .post("/eip/route/step")
                .then()
                .statusCode(200)
                .body(Matchers.is("Hello Monty from step!"));

    }

}
