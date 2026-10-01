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
package org.apache.camel.quarkus.component.cli.connector.it;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.http.ServerWebSocket;
import org.apache.camel.util.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.apache.camel.quarkus.component.cli.connector.it.CliToolServer.action;
import static org.apache.camel.quarkus.component.cli.connector.it.CliToolServer.awaitResult;
import static org.apache.camel.quarkus.component.cli.connector.it.CliToolServer.reconnect;
import static org.apache.camel.quarkus.component.cli.connector.it.CliToolServer.send;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * The Vert.x client, the default on Camel Quarkus.
 */
@QuarkusTest
@QuarkusTestResource(CliToolServer.class)
class CliConnectorTest {

    private static final Path LOG = Paths.get("target/quarkus.log");

    @Test
    void connectsWithTheExpectedClient() throws Exception {
        JsonObject hello = reconnect();

        assertThat(hello.getString("transport")).isEqualTo("vertx");
        assertThat(hello.getString("camelVersion")).isNotBlank();
        // the url is used as given, encoded characters included
        assertThat(CliToolServer.REQUEST_URIS).isNotEmpty().allSatisfy(uri -> assertThat(uri)
                .isEqualTo(CliToolServer.REQUEST_URI));
    }

    @Test
    void executesActions() throws Exception {
        reconnect();

        send(action("r1", "send", "endpoint", "direct:echo", "body", "World", "exchangePattern", "InOut"));
        JsonObject result = awaitResult("r1");
        assertThat(result.getBoolean("ok")).isTrue();
        assertThat(resultJson(result)).contains("Hello World");

        send(action("r2", "does-not-exist"));
        result = awaitResult("r2");
        assertThat(result.getBoolean("ok")).isFalse();
        assertThat(result.getString("error")).isEqualTo("Unknown action: does-not-exist");
    }

    @Test
    void exchangesLargeMessages() throws Exception {
        reconnect();
        // well over the 256 KB messages and the 64 KB frames that Vert.x accepts by default
        String body = "x".repeat(4 * 1024 * 1024);

        // each of them in 64 frames: more frames in total than a client fetching a fixed number of frames would read
        for (int i = 0; i < 6; i++) {
            send(action("r" + i, "send", "endpoint", "direct:length", "body", body, "exchangePattern", "InOut"));
            JsonObject result = awaitResult("r" + i);
            assertThat(result.getBoolean("ok")).isTrue();
            assertThat(resultJson(result)).contains("length=" + body.length());
        }

        // and back: trace snapshots are sent in messages of up to 128 KB, over the 64 KB frames the tool accepts
        String traced = "y".repeat(6000);
        for (int i = 0; i < 40; i++) {
            send(action("t" + i, "send", "endpoint", "direct:echo", "body", traced, "exchangePattern", "InOut"));
            assertThat(awaitResult("t" + i).getBoolean("ok")).isTrue();
        }
        await().atMost(20, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(CliToolServer.LARGEST_TRACE_SNAPSHOT).hasValueGreaterThan(64 * 1024));
    }

    @Test
    void reconnectsOnceTheTokenIsAccepted() throws Exception {
        reconnect();
        int rejected = CliToolServer.REJECTED.get();
        try {
            CliToolServer.reject = true;
            CliToolServer.SOCKETS.forEach(ServerWebSocket::close);

            await().atMost(10, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(CliToolServer.REJECTED).hasValueGreaterThan(rejected + 1));
            // the client reports the HTTP status of the rejected handshake
            await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(Files.readString(LOG))
                    .contains("Camel CLI connector was rejected by ws://127.0.0.1:")
                    .contains("(HTTP 401): check camel.cli.websocket.token"));
        } finally {
            CliToolServer.reject = false;
        }
        // connected again once the token is accepted
        assertThat(CliToolServer.awaitFrame(f -> "hello".equals(f.getString("type")))).isNotNull();
    }

    @Test
    void receivesLargeActionsInASingleFrame() throws Exception {
        reconnect();
        // over the 64 KB frames that Vert.x accepts by default
        String body = "x".repeat(1024 * 1024);

        CliToolServer.sendInASingleFrame(
                action("r1", "send", "endpoint", "direct:length", "body", body, "exchangePattern", "InOut"));
        JsonObject result = awaitResult("r1");
        assertThat(result.getBoolean("ok")).isTrue();
        assertThat(resultJson(result)).contains("length=" + body.length());
    }

    @Test
    void staysConnected() throws Exception {
        reconnect();
        int connections = CliToolServer.CONNECTIONS.get();

        // longer than the connect and handshake timeouts (10 s), which must not apply once connected
        await().during(15, TimeUnit.SECONDS).atMost(20, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(CliToolServer.CONNECTIONS).hasValue(connections));
    }

    @Test
    void reconnectsWhenTheToolDoesNotAnswerTheHandshake() throws Exception {
        reconnect();
        int unanswered = CliToolServer.UNANSWERED.get();
        try {
            CliToolServer.silent = true;
            CliToolServer.SOCKETS.forEach(ServerWebSocket::close);

            // the handshake times out after 10 s, and the connector tries again
            await().atMost(40, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(CliToolServer.UNANSWERED).hasValueGreaterThan(unanswered + 1));
        } finally {
            CliToolServer.silent = false;
        }
        assertThat(CliToolServer.awaitFrame(f -> "hello".equals(f.getString("type")))).isNotNull();
    }

    private static String resultJson(JsonObject result) {
        JsonObject json = result.getMap("result");
        return json.toJson();
    }
}
