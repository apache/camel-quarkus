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
package org.apache.camel.quarkus.support.reactor.netty.deployment;

import java.util.stream.Stream;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

public class ReactorNettySupportProcessor {

    @BuildStep
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses) {
        Stream.of(

                /* The following io.netty.util.* items were not accepted
                 * to quarkus via https://github.com/quarkusio/quarkus/pull/14994
                 * Keeping them here for now */
                io.netty.util.NetUtil.class.getName(),
                "io.netty.channel.socket.nio.ProtocolFamilyConverter$1",
                io.netty.handler.ssl.OpenSsl.class.getName(),
                "io.netty.internal.tcnative.SSL",
                "io.netty.resolver.dns.PreferredAddressTypeComparator$1",

                reactor.netty.http.client.HttpClient.class.getName(),
                "reactor.netty.http.client.HttpClientSecure",
                reactor.netty.tcp.TcpClient.class.getName(),
                "reactor.netty.tcp.TcpClientSecure",
                "reactor.netty.resources.DefaultLoopNativeDetector",
                "reactor.netty.resources.DefaultLoopEpoll",
                "reactor.netty.resources.DefaultLoopKQueue",
                "reactor.netty.resources.MicrometerPooledConnectionProviderMeterRegistrar",
                "reactor.netty.Metrics")
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeInitializedClasses::produce);
    }

    @BuildStep
    void reflectiveClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {

        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "reactor.netty.channel.BootstrapHandlers$BootstrapInitializerHandler",
                "reactor.netty.channel.ChannelOperationsHandler",
                "reactor.netty.resources.PooledConnectionProvider$PooledConnectionAllocator$PooledConnectionInitializer",
                "reactor.netty.tcp.SslProvider$SslReadHandler").methods(true).fields(false).build());

    }

}
