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
package org.apache.camel.quarkus.component.kudu.deployment;

import java.util.Arrays;
import java.util.List;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.UnsafeAccessedFieldBuildItem;
import org.apache.camel.quarkus.component.kudu.graal.EmptyByteBufStub;
import org.apache.kudu.shaded.io.netty.util.internal.logging.InternalLoggerFactory;
import org.jboss.logging.Logger;

class KuduProcessor {
    private static final Logger LOGGER = Logger.getLogger(KuduProcessor.class);
    static {
        InternalLoggerFactory.setDefaultFactory(new JBossNettyLoggerFactory());
    }

    private static final String FEATURE = "camel-kudu";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    void reflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false,
                "org.apache.kudu.tserver.Tserver$ResourceMetricsPB",
                "org.apache.kudu.tserver.Tserver$ResourceMetricsPB$Builder"));
    }

    /*
     * Adapted from https://github.com/quarkusio/quarkus/blob/master/extensions/netty/deployment/src/main/java/io/quarkus/netty/deployment/NettyProcessor.java
     */
    @BuildStep
    void netty(
            BuildProducer<NativeImageConfigBuildItem> nativeImageConfig,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false,
                "org.apache.kudu.tserver.Tserver$ResourceMetricsPB",
                "org.apache.kudu.tserver.Tserver$ResourceMetricsPB$Builder"));

        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false,
                "org.apache.kudu.shaded.io.netty.channel.socket.nio.NioSocketChannel"));
        reflectiveClass
                .produce(new ReflectiveClassBuildItem(false, false,
                        "org.apache.kudu.shaded.io.netty.channel.socket.nio.NioServerSocketChannel"));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, "java.util.LinkedHashMap"));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, "sun.nio.ch.SelectorImpl"));

        NativeImageConfigBuildItem.Builder builder = NativeImageConfigBuildItem.builder()
                //.addNativeImageSystemProperty("io.netty.noUnsafe", "true")
                // Use small chunks to avoid a lot of wasted space. Default is 16mb * arenas (derived from core count)
                // Since buffers are cached to threads, the malloc overhead is temporary anyway
                .addNativeImageSystemProperty("io.netty.allocator.maxOrder", "1")
                .addRuntimeInitializedClass("org.apache.kudu.shaded.io.netty.handler.ssl.JdkNpnApplicationProtocolNegotiator")
                .addRuntimeInitializedClass("org.apache.kudu.shaded.io.netty.handler.ssl.ConscryptAlpnSslEngine")
                .addRuntimeInitializedClass("org.apache.kudu.shaded.io.netty.handler.ssl.ReferenceCountedOpenSslEngine")
                .addRuntimeInitializedClass("org.apache.kudu.shaded.io.netty.handler.ssl.ReferenceCountedOpenSslContext")
                .addRuntimeInitializedClass("org.apache.kudu.shaded.io.netty.handler.ssl.ReferenceCountedOpenSslClientContext")
                .addRuntimeInitializedClass("org.apache.kudu.shaded.io.netty.handler.ssl.util.ThreadLocalInsecureRandom")
                .addRuntimeInitializedClass("org.apache.kudu.shaded.io.netty.buffer.ByteBufUtil$HexUtil")
                .addRuntimeInitializedClass("org.apache.kudu.shaded.io.netty.buffer.PooledByteBufAllocator")
                .addRuntimeInitializedClass("org.apache.kudu.shaded.io.netty.buffer.ByteBufAllocator")
                .addRuntimeInitializedClass("org.apache.kudu.shaded.io.netty.buffer.ByteBufUtil")
                .addNativeImageSystemProperty("io.netty.leakDetection.level", "DISABLED");

        try {
            Class.forName("org.apache.kudu.shaded.io.netty.handler.codec.http.HttpObjectEncoder");
            builder
                    .addRuntimeInitializedClass("org.apache.kudu.shaded.io.netty.handler.codec.http.HttpObjectEncoder")
                    .addRuntimeInitializedClass(
                            "org.apache.kudu.shaded.io.netty.handler.codec.http.websocketx.extensions.compression.DeflateDecoder")
                    .addRuntimeInitializedClass(
                            "org.apache.kudu.shaded.io.netty.handler.codec.http.websocketx.WebSocket00FrameEncoder");
        } catch (ClassNotFoundException e) {
            //ignore
            LOGGER.debug("Not registering Netty HTTP classes as they were not found");
        }

        try {
            Class.forName("org.apache.kudu.shaded.io.netty.handler.codec.http2.Http2CodecUtil");
            builder
                    .addRuntimeInitializedClass("org.apache.kudu.shaded.io.netty.handler.codec.http2.Http2CodecUtil")
                    .addRuntimeInitializedClass("org.apache.kudu.shaded.io.netty.handler.codec.http2.Http2ClientUpgradeCodec")
                    .addRuntimeInitializedClass("org.apache.kudu.shaded.io.netty.handler.codec.http2.DefaultHttp2FrameWriter")
                    .addRuntimeInitializedClass("org.apache.kudu.shaded.io.netty.handler.codec.http2.Http2ConnectionHandler");
        } catch (ClassNotFoundException e) {
            //ignore
            LOGGER.debug("Not registering Netty HTTP2 classes as they were not found");
        }

        try {
            Class.forName("org.apache.kudu.shaded.io.netty.channel.unix.UnixChannel");
            builder.addRuntimeInitializedClass("org.apache.kudu.shaded.io.netty.channel.unix.Errors")
                    .addRuntimeInitializedClass("org.apache.kudu.shaded.io.netty.channel.unix.FileDescriptor")
                    .addRuntimeInitializedClass("org.apache.kudu.shaded.io.netty.channel.unix.IovArray")
                    .addRuntimeInitializedClass("org.apache.kudu.shaded.io.netty.channel.unix.Limits");
        } catch (ClassNotFoundException e) {
            //ignore
            LOGGER.debug("Not registering Netty native unix classes as they were not found");
        }

        try {
            Class.forName("org.apache.kudu.shaded.io.netty.channel.epoll.EpollMode");
            builder.addRuntimeInitializedClass("org.apache.kudu.shaded.io.netty.channel.epoll.Epoll")
                    .addRuntimeInitializedClass("org.apache.kudu.shaded.io.netty.channel.epoll.EpollEventArray")
                    .addRuntimeInitializedClass("org.apache.kudu.shaded.io.netty.channel.epoll.EpollEventLoop")
                    .addRuntimeInitializedClass("org.apache.kudu.shaded.io.netty.channel.epoll.Native");
        } catch (ClassNotFoundException e) {
            //ignore
            LOGGER.debug("Not registering Netty native epoll classes as they were not found");
        }

        try {
            Class.forName("org.apache.kudu.shaded.io.netty.channel.kqueue.AcceptFilter");
            builder.addRuntimeInitializedClass("org.apache.kudu.shaded.io.netty.channel.kqueue.KQueue")
                    .addRuntimeInitializedClass("org.apache.kudu.shaded.io.netty.channel.kqueue.KQueueEventArray")
                    .addRuntimeInitializedClass("org.apache.kudu.shaded.io.netty.channel.kqueue.KQueueEventLoop")
                    .addRuntimeInitializedClass("org.apache.kudu.shaded.io.netty.channel.kqueue.Native");
        } catch (ClassNotFoundException e) {
            //ignore
            LOGGER.debug("Not registering Netty native kqueue classes as they were not found");
        }

        nativeImageConfig.produce(builder.build());
    }

    /*
     * Adapted from https://github.com/quarkusio/quarkus/blob/master/extensions/netty/deployment/src/main/java/io/quarkus/netty/deployment/NettyProcessor.java
     */
    @BuildStep
    public RuntimeReinitializedClassBuildItem nettyReinitScheduledFutureTask() {
        return new RuntimeReinitializedClassBuildItem(
                "org.apache.camel.quarkus.component.kudu.graal.Holder_io_netty_util_concurrent_ScheduledFutureTask");
    }

    /*
     * Adapted from https://github.com/quarkusio/quarkus/blob/master/extensions/netty/deployment/src/main/java/io/quarkus/netty/deployment/NettyProcessor.java
     */
    @BuildStep
    public List<UnsafeAccessedFieldBuildItem> nettyUnsafeAccessedFields() {
        return Arrays.asList(
                new UnsafeAccessedFieldBuildItem("sun.nio.ch.SelectorImpl", "selectedKeys"),
                new UnsafeAccessedFieldBuildItem("sun.nio.ch.SelectorImpl", "publicSelectedKeys"),

                new UnsafeAccessedFieldBuildItem(
                        "org.apache.kudu.shaded.io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueueProducerIndexField",
                        "producerIndex"),
                new UnsafeAccessedFieldBuildItem(
                        "org.apache.kudu.shaded.io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueueProducerLimitField",
                        "producerLimit"),
                new UnsafeAccessedFieldBuildItem(
                        "org.apache.kudu.shaded.io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueueConsumerIndexField",
                        "consumerIndex"),

                new UnsafeAccessedFieldBuildItem(
                        "org.apache.kudu.shaded.io.netty.util.internal.shaded.org.jctools.queues.BaseMpscLinkedArrayQueueProducerFields",
                        "producerIndex"),
                new UnsafeAccessedFieldBuildItem(
                        "org.apache.kudu.shaded.io.netty.util.internal.shaded.org.jctools.queues.BaseMpscLinkedArrayQueueColdProducerFields",
                        "producerLimit"),
                new UnsafeAccessedFieldBuildItem(
                        "org.apache.kudu.shaded.io.netty.util.internal.shaded.org.jctools.queues.BaseMpscLinkedArrayQueueConsumerFields",
                        "consumerIndex"));
    }

    /*
     * Adapted from https://github.com/quarkusio/quarkus/blob/master/extensions/netty/deployment/src/main/java/io/quarkus/netty/deployment/NettyProcessor.java
     */
    @BuildStep
    RuntimeInitializedClassBuildItem nettyRuntimeInitBcryptUtil() {
        // this holds a direct allocated byte buffer that needs to be initialised at run time
        return new RuntimeInitializedClassBuildItem(EmptyByteBufStub.class.getName());
    }

}
