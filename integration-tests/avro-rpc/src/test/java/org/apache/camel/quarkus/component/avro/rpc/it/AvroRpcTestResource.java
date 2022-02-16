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

import java.net.InetSocketAddress;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.jetty.HttpServer;
import org.apache.avro.ipc.netty.NettyServer;
import org.apache.avro.ipc.reflect.ReflectResponder;
import org.apache.avro.ipc.specific.SpecificResponder;
import org.apache.camel.quarkus.component.avro.rpc.it.reflection.TestReflection;
import org.apache.camel.quarkus.component.avro.rpc.it.reflection.impl.TestReflectionImpl;
import org.apache.camel.quarkus.component.avro.rpc.it.specific.generated.KeyValueProtocol;
import org.apache.camel.quarkus.component.avro.rpc.it.specific.impl.KeyValueProtocolImpl;
import org.apache.camel.quarkus.test.AvailablePortFinder;
import org.apache.camel.util.CollectionHelper;

public class AvroRpcTestResource implements QuarkusTestResourceLifecycleManager {

    //server implementations
    TestReflection httpTestReflection = new TestReflectionImpl();
    TestReflection nettyTestReflection = new TestReflectionImpl();
    KeyValueProtocolImpl httpKeyValue = new KeyValueProtocolImpl();
    KeyValueProtocolImpl nettyKeyValue = new KeyValueProtocolImpl();

    //avro servers listening on localhost
    Server reflectHttpServer, reflectNettyServer, specificHttpServer, specificNettyServer;

    @Override
    public Map<String, String> start() {
        try {

            // ---------------- producers ---------------
            final int reflectiveHttpPort = AvailablePortFinder.getNextAvailable();
            reflectHttpServer = new HttpServer(
                    new ReflectResponder(TestReflection.class, httpTestReflection),
                    reflectiveHttpPort);
            reflectHttpServer.start();

            final int reflectiveNettyPort = AvailablePortFinder.getNextAvailable();
            reflectNettyServer = new NettyServer(
                    new ReflectResponder(TestReflection.class, nettyTestReflection),
                    new InetSocketAddress(reflectiveNettyPort));
            reflectNettyServer.start();

            final int specificHttpPort = AvailablePortFinder.getNextAvailable();
            specificHttpServer = new HttpServer(
                    new SpecificResponder(KeyValueProtocol.class, httpKeyValue),
                    specificHttpPort);
            specificHttpServer.start();

            final int specificNettyPort = AvailablePortFinder.getNextAvailable();
            specificNettyServer = new NettyServer(
                    new SpecificResponder(KeyValueProtocol.class, nettyKeyValue),
                    new InetSocketAddress(specificNettyPort));
            specificNettyServer.start();

            //----------- consumers ----------------------------------

            final int reflectiveHttpTransceiverPort = AvailablePortFinder.getNextAvailable();
            final int reflectiveNettyTransceiverPort = AvailablePortFinder.getNextAvailable();
            final int specificHttpTransceiverPort = AvailablePortFinder.getNextAvailable();
            final int specificNettyTransceiverPort = AvailablePortFinder.getNextAvailable();

            return CollectionHelper.mapOf(AvroRpcResource.REFLECTIVE_HTTP_SERVER_PORT_PARAM, String.valueOf(reflectiveHttpPort),
                    AvroRpcResource.REFLECTIVE_NETTY_SERVER_PORT_PARAM, String.valueOf(reflectiveNettyPort),
                    AvroRpcResource.SPECIFIC_HTTP_SERVER_PORT_PARAM, String.valueOf(specificHttpPort),
                    AvroRpcResource.SPECIFIC_NETTY_SERVER_PORT_PARAM, String.valueOf(specificNettyPort),
                    AvroRpcResource.REFLECTIVE_HTTP_TRANSCEIVER_PORT_PARAM, String.valueOf(reflectiveHttpTransceiverPort),
                    AvroRpcResource.REFLECTIVE_NETTY_TRANSCEIVER_PORT_PARAM, String.valueOf(reflectiveNettyTransceiverPort),
                    AvroRpcResource.SPECIFIC_HTTP_TRANSCEIVER_PORT_PARAM, String.valueOf(specificHttpTransceiverPort),
                    AvroRpcResource.SPECIFIC_NETTY_TRANSCEIVER_PORT_PARAM, String.valueOf(specificNettyTransceiverPort));
        } catch (Exception e) {
            throw new RuntimeException("Could not start avro-rpc server", e);
        }
    }

    @Override
    public void stop() {
        if (reflectHttpServer != null) {
            reflectHttpServer.close();
        }
        if (reflectNettyServer != null) {
            reflectNettyServer.close();
        }
        if (specificHttpServer != null) {
            specificHttpServer.close();
        }
        if (specificNettyServer != null) {
            specificNettyServer.close();
        }
        AvailablePortFinder.releaseReservedPorts();
    }

    @Override
    public void inject(Object testInstance) {
        AvroRpcTestSupport testSupport = (AvroRpcTestSupport) testInstance;
        if (testSupport.isHttp()) {
            testSupport.setKeyValueProtocol(httpKeyValue);
            testSupport.setTestReflection(httpTestReflection);
        } else {
            testSupport.setKeyValueProtocol(nettyKeyValue);
            testSupport.setTestReflection(nettyTestReflection);
        }
    }
}
