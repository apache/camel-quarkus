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
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

public class ReactorNettySupportProcessor {
    static final String FEATURE = "camel-support-reactor-netty";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses) {
        Stream.of(

                /* The following io.netty.util.* items were not accepted
                 * to quarkus via https://github.com/quarkusio/quarkus/pull/14994
                 * Keeping them here for now */
                "io.netty.util.NetUtil",
                "io.netty.channel.socket.InternetProtocolFamily",
                "io.netty.handler.ssl.OpenSsl",
                "io.netty.channel.socket.nio.ProtocolFamilyConverter$1",
                "io.netty.internal.tcnative.SSL",

                "reactor.netty.http.client.HttpClient",
                "reactor.netty.tcp.TcpClient",
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

        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false,
                "reactor.netty.channel.BootstrapHandlers$BootstrapInitializerHandler",
                "reactor.netty.channel.ChannelOperationsHandler",
                "reactor.netty.resources.PooledConnectionProvider$PooledConnectionAllocator$PooledConnectionInitializer",
                "reactor.netty.tcp.SslProvider$SslReadHandler"));

    }

}
