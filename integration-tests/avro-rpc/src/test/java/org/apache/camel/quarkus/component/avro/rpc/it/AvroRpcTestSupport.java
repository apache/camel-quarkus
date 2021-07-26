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
package org.apache.camel.quarkus.component.avro.rpc.it;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;

import io.quarkus.test.common.QuarkusTestResource;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.avro.ipc.HttpTransceiver;
import org.apache.avro.ipc.Requestor;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.netty.NettyTransceiver;
import org.apache.avro.ipc.reflect.ReflectRequestor;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.camel.quarkus.component.avro.rpc.it.reflection.TestPojo;
import org.apache.camel.quarkus.component.avro.rpc.it.reflection.TestReflection;
import org.apache.camel.quarkus.component.avro.rpc.it.specific.generated.Key;
import org.apache.camel.quarkus.component.avro.rpc.it.specific.generated.KeyValueProtocol;
import org.apache.camel.quarkus.component.avro.rpc.it.specific.generated.Value;
import org.apache.camel.quarkus.component.avro.rpc.it.specific.impl.KeyValueProtocolImpl;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterEach;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTestResource(AvroRpcTestResource.class)
abstract class AvroRpcTestSupport {

    private final static String NAME = "Sheldon";
    public static final String NAME_FROM_KEY_VALUE = "{\"value\": \"" + NAME + "\"}";

    private TestReflection testReflection;

    private KeyValueProtocol keyValueProtocol;

    private final ProtocolType protocol;

    private Requestor reflectRequestor, specificRequestor;
    private Transceiver reflectTransceiver, specificTransceiver;

    //@Test
    public void testReflectionProducer() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("protocol", protocol)
                .body(NAME)
                .post("/avro-rpc/reflectionProducerSet")
                .then()
                .statusCode(204);

        assertEquals(NAME, testReflection.getName());

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(protocol)
                .post("/avro-rpc/reflectionProducerGet")
                .then()
                .statusCode(200)
                .body(is(NAME));
    }

    //@Test
    public void testSpecificProducer() throws InterruptedException {
        Key key = Key.newBuilder().setKey("1").build();
        Value value = Value.newBuilder().setValue(NAME).build();

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("protocol", protocol)
                .queryParam("key", key.getKey().toString())
                .body(value.getValue().toString())
                .post("/avro-rpc/specificProducerPut")
                .then()
                .statusCode(204);

        assertEquals(value, ((KeyValueProtocolImpl) keyValueProtocol).getStore().get(key));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("protocol", protocol)
                .body(key.getKey().toString())
                .post("/avro-rpc/specificProducerGet")
                .then()
                .statusCode(200)
                .body(is(NAME_FROM_KEY_VALUE));
    }

    //@Test
    public void testReflectionConsumer() throws Exception {
        TestPojo testPojo = new TestPojo();
        testPojo.setPojoName(NAME);
        Object[] request = { testPojo };

        initReflectRequestor();
        reflectRequestor.request("setTestPojo", request);

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(protocol)
                .post("/avro-rpc/reflectionConsumerGet")
                .then()
                .statusCode(200)
                .body(is(NAME));
    }

    //@Test
    public void testSpecificConsumer() throws Exception {
        Key key = Key.newBuilder().setKey("2").build();
        Value value = Value.newBuilder().setValue(NAME).build();

        initSpecificRequestor();
        specificRequestor.request("put", new Object[] { key, value });

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("protocol", protocol)
                .body(key.getKey().toString())
                .post("/avro-rpc/specificConsumerGet")
                .then()
                .statusCode(200)
                .body(is(NAME_FROM_KEY_VALUE));
    }

    public AvroRpcTestSupport(ProtocolType protocol) {
        this.protocol = protocol;
    }

    public void setTestReflection(TestReflection testReflection) {
        this.testReflection = testReflection;
    }

    public void setKeyValueProtocol(KeyValueProtocol keyValueProtocol) {
        this.keyValueProtocol = keyValueProtocol;
    }

    boolean isHttp() {
        return ProtocolType.http == protocol;
    }

    void initReflectRequestor() throws IOException {
        if (reflectRequestor == null) {
            if (isHttp()) {
                reflectTransceiver = new HttpTransceiver(
                        new URL("http://localhost:"
                                + ConfigProvider.getConfig().getValue(AvroRpcResource.REFLECTIVE_HTTP_TRANSCEIVER_PORT_PARAM,
                                        String.class)));
            } else {
                reflectTransceiver = new NettyTransceiver(
                        new InetSocketAddress("localhost",
                                ConfigProvider.getConfig().getValue(AvroRpcResource.REFLECTIVE_NETTY_TRANSCEIVER_PORT_PARAM,
                                        Integer.class)));
            }
            reflectRequestor = new ReflectRequestor(TestReflection.class, reflectTransceiver);
        }
    }

    void initSpecificRequestor() throws IOException {
        if (specificRequestor == null) {
            if (isHttp()) {
                specificTransceiver = new HttpTransceiver(
                        new URL("http://localhost:"
                                + ConfigProvider.getConfig().getValue(AvroRpcResource.SPECIFIC_HTTP_TRANSCEIVER_PORT_PARAM,
                                        String.class)));
            } else {
                specificTransceiver = new NettyTransceiver(
                        new InetSocketAddress("localhost",
                                ConfigProvider.getConfig().getValue(AvroRpcResource.SPECIFIC_NETTY_TRANSCEIVER_PORT_PARAM,
                                        Integer.class)));
            }
            specificRequestor = new SpecificRequestor(KeyValueProtocol.class, specificTransceiver);
        }
    }

    @AfterEach
    public void tearDown() throws Exception {

        if (specificTransceiver != null) {
            specificTransceiver.close();
            specificTransceiver = null;
            specificRequestor = null;
        }

        if (reflectTransceiver != null) {
            reflectTransceiver.close();
            reflectTransceiver = null;
            reflectRequestor = null;
        }
    }

}
