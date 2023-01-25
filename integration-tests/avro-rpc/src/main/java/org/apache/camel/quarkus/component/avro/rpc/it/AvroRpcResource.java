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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.component.avro.rpc.it.reflection.TestPojo;
import org.apache.camel.quarkus.component.avro.rpc.it.reflection.TestReflection;
import org.apache.camel.quarkus.component.avro.rpc.it.reflection.impl.TestReflectionImpl;
import org.apache.camel.quarkus.component.avro.rpc.it.specific.generated.Key;
import org.apache.camel.quarkus.component.avro.rpc.it.specific.generated.KeyValueProtocol;
import org.apache.camel.quarkus.component.avro.rpc.it.specific.generated.Value;
import org.apache.camel.quarkus.component.avro.rpc.it.specific.impl.KeyValueProtocolImpl;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/avro-rpc")
@ApplicationScoped
public class AvroRpcResource {

    public static final String REFLECTIVE_HTTP_SERVER_PORT_PARAM = "camel.avro-rpc.test.reflective.httpServerReflection.port";
    public static final String REFLECTIVE_NETTY_SERVER_PORT_PARAM = "camel.avro-rpc.test.reflective.nettyServerReflection.port";
    public static final String SPECIFIC_HTTP_SERVER_PORT_PARAM = "camel.avro-rpc.test.generated.httpServerReflection.port";
    public static final String SPECIFIC_NETTY_SERVER_PORT_PARAM = "camel.avro-rpc.test.generated.nettyServerReflection.port";
    public static final String REFLECTIVE_HTTP_TRANSCEIVER_PORT_PARAM = "camel.avro-rpc.test.httpTransceiverReflection.port";
    public static final String REFLECTIVE_NETTY_TRANSCEIVER_PORT_PARAM = "camel.avro-rpc.test.nettyTransceiverReflection.port";
    public static final String SPECIFIC_HTTP_TRANSCEIVER_PORT_PARAM = "camel.avro-rpc.test.specific.httpTransceiverReflection.port";
    public static final String SPECIFIC_NETTY_TRANSCEIVER_PORT_PARAM = "camel.avro-rpc.test.specific.nettyTransceiverReflection.port";

    private TestReflection httpTestReflection = new TestReflectionImpl(),
            nettyTestReflection = new TestReflectionImpl();
    private KeyValueProtocol httpKeyValue = new KeyValueProtocolImpl(),
            nettyKeyValue = new KeyValueProtocolImpl();

    @Inject
    ProducerTemplate producerTemplate;

    @ConfigProperty(name = REFLECTIVE_HTTP_SERVER_PORT_PARAM)
    Integer reflectiveHttpPort;

    @ConfigProperty(name = REFLECTIVE_NETTY_SERVER_PORT_PARAM)
    Integer reflectiveNettyPort;

    @ConfigProperty(name = SPECIFIC_HTTP_SERVER_PORT_PARAM)
    Integer specificHttpPort;

    @ConfigProperty(name = SPECIFIC_NETTY_SERVER_PORT_PARAM)
    Integer specificNettyPort;

    @Path("/reflectionProducerSet")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void reflectionProducerSet(@QueryParam("protocol") ProtocolType protocol, String name) throws Exception {
        Object[] request = { name };
        producerTemplate.requestBody(String.format(
                "avro:%s:localhost:%d/setName?protocolClassName=%s&singleParameter=true",
                protocol,
                protocol == ProtocolType.http ? reflectiveHttpPort : reflectiveNettyPort,
                TestReflection.class.getCanonicalName()), request);
    }

    @Path("/reflectionProducerGet")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String reflectionProducerGet(ProtocolType protocol) throws Exception {
        return producerTemplate.requestBody(String.format(
                "avro:%s:localhost:%d/getName?protocolClassName=%s",
                protocol,
                protocol == ProtocolType.http ? reflectiveHttpPort : reflectiveNettyPort,
                TestReflection.class.getCanonicalName()), null, String.class);
    }

    @Path("/specificProducerPut")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public void specificProducerPut(@QueryParam("protocol") ProtocolType protocol, @QueryParam("key") String key, String value)
            throws Exception {
        Key k = Key.newBuilder().setKey(key).build();
        Value v = Value.newBuilder().setValue(value).build();

        Object[] request = { k, v };
        producerTemplate.requestBody(String.format(
                "avro:%s:localhost:%d/put?protocolClassName=%s",
                protocol,
                protocol == ProtocolType.http ? specificHttpPort : specificNettyPort,
                KeyValueProtocol.class.getCanonicalName()), request);
    }

    @Path("/specificProducerGet")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String specificProducerGet(@QueryParam("protocol") ProtocolType protocol, String key) throws Exception {
        Key k = Key.newBuilder().setKey(key).build();

        Object[] request = { k };
        return producerTemplate.requestBody(String.format(
                "avro:%s:localhost:%d/get?protocolClassName=%s&singleParameter=true",
                protocol,
                protocol == ProtocolType.http ? specificHttpPort : specificNettyPort,
                KeyValueProtocol.class.getCanonicalName()), request, String.class);
    }

    @Path("/reflectionConsumerGet")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String reflectionConsumerGet(ProtocolType protocol) throws Exception {
        TestPojo testPojo = getTestReflection(protocol).getTestPojo();
        return testPojo != null ? testPojo.getPojoName() : null;
    }

    @Path("/specificConsumerGet")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String specificConsumerGet(@QueryParam("protocol") ProtocolType protocol, String key) throws Exception {
        Key k = Key.newBuilder().setKey(key).build();

        return getKeyValue(protocol).get(k).toString();
    }

    public TestReflection getTestReflection(ProtocolType protocol) {
        return protocol == ProtocolType.http ? httpTestReflection : nettyTestReflection;
    }

    public KeyValueProtocol getKeyValue(ProtocolType protocol) {
        return protocol == ProtocolType.http ? httpKeyValue : nettyKeyValue;
    }
}
