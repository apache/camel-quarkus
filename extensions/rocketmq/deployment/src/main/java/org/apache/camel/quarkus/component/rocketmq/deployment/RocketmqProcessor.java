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
package org.apache.camel.quarkus.component.rocketmq.deployment;

import java.util.List;

import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.util.ASMClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedPackageBuildItem;
import org.apache.camel.quarkus.component.rocketmq.runtime.CamelRocketmqRecorder;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;

class RocketmqProcessor {

    private static final String FEATURE = "camel-rocketmq";

    private static final List<String> ROCKETMQ_REFLECTIVE_CLASSES = List.of(
            "org.apache.rocketmq.client.ClientConfig",
            "org.apache.rocketmq.client.consumer.DefaultMQPushConsumer",
            "org.apache.rocketmq.common.message.MessageQueue",
            "org.apache.rocketmq.remoting.protocol.RemotingCommand",
            "org.apache.rocketmq.remoting.protocol.RemotingSerializable",
            "org.apache.rocketmq.remoting.protocol.body.ConsumeStatus",
            "org.apache.rocketmq.remoting.protocol.body.ConsumerRunningInfo",
            "org.apache.rocketmq.remoting.protocol.body.ProcessQueueInfo",
            "org.apache.rocketmq.remoting.protocol.header.GetConsumerListByGroupRequestHeader",
            "org.apache.rocketmq.remoting.protocol.header.GetConsumerListByGroupResponseBody",
            "org.apache.rocketmq.remoting.protocol.header.GetConsumerRunningInfoRequestHeader",
            "org.apache.rocketmq.remoting.protocol.header.HeartbeatRequestHeader",
            "org.apache.rocketmq.remoting.protocol.header.NotifyConsumerIdsChangedRequestHeader",
            "org.apache.rocketmq.remoting.protocol.header.PullMessageRequestHeader",
            "org.apache.rocketmq.remoting.protocol.header.QueryConsumerOffsetRequestHeader",
            "org.apache.rocketmq.remoting.protocol.header.QueryConsumerOffsetResponseHeader",
            "org.apache.rocketmq.remoting.protocol.header.SendMessageRequestHeaderV2",
            "org.apache.rocketmq.remoting.protocol.header.UnregisterClientRequestHeader",
            "org.apache.rocketmq.remoting.protocol.header.UpdateConsumerOffsetRequestHeader",
            "org.apache.rocketmq.remoting.protocol.header.namesrv.GetRouteInfoRequestHeader",
            "org.apache.rocketmq.remoting.protocol.heartbeat.ConsumerData",
            "org.apache.rocketmq.remoting.protocol.heartbeat.HeartbeatData",
            "org.apache.rocketmq.remoting.protocol.heartbeat.ProducerData",
            "org.apache.rocketmq.remoting.protocol.heartbeat.SubscriptionData",
            "org.apache.rocketmq.remoting.protocol.route.BrokerData",
            "org.apache.rocketmq.remoting.protocol.route.QueueData",
            "org.apache.rocketmq.remoting.protocol.route.TopicRouteData",
            "org.apache.rocketmq.remoting.rpc.RpcRequestHeader",
            "org.apache.rocketmq.remoting.rpc.TopicQueueRequestHeader",
            "org.apache.rocketmq.remoting.rpc.TopicRequestHeader");

    private static final List<String> ROCKETMQ_NETTY_REFLECTIVE_CLASSES = List.of(
            "org.apache.rocketmq.common.logging.DefaultJoranConfiguratorExt",
            "org.apache.rocketmq.remoting.netty.NettyDecoder",
            "org.apache.rocketmq.remoting.netty.NettyEncoder",
            "org.apache.rocketmq.remoting.netty.NettyRemotingClient$2",
            "org.apache.rocketmq.remoting.netty.NettyRemotingClient$NettyClientHandler",
            "org.apache.rocketmq.remoting.netty.NettyRemotingClient$NettyConnectManageHandler",
            "org.apache.rocketmq.remoting.protocol.header.PullMessageResponseHeader",
            "org.apache.rocketmq.remoting.protocol.header.SendMessageResponseHeader");

    private static final List<String> ROCKETMQ_REFLECTIVE_ENUMS = List.of(
            "org.apache.rocketmq.common.consumer.ConsumeFromWhere",
            "org.apache.rocketmq.remoting.protocol.LanguageCode",
            "org.apache.rocketmq.remoting.protocol.RequestCode",
            "org.apache.rocketmq.remoting.protocol.ResponseCode",
            "org.apache.rocketmq.remoting.protocol.SerializeType",
            "org.apache.rocketmq.remoting.protocol.heartbeat.ConsumeType",
            "org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void configureFastJson(CamelRocketmqRecorder recorder) {
        recorder.configureFastJson();
    }

    @BuildStep
    void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(ROCKETMQ_REFLECTIVE_CLASSES)
                .fields()
                .methods()
                .constructors()
                .build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(ROCKETMQ_NETTY_REFLECTIVE_CLASSES)
                .methods()
                .constructors()
                .build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(ROCKETMQ_REFLECTIVE_ENUMS)
                .fields()
                .build());
    }

    @BuildStep
    void registerResources(BuildProducer<NativeImageResourceBuildItem> resource) {
        resource.produce(new NativeImageResourceBuildItem(
                "META-INF/services/org.apache.rocketmq.common.namesrv.TopAddressing"));
    }

    @BuildStep
    void registerRuntimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses,
            BuildProducer<RuntimeInitializedPackageBuildItem> runtimeInitializedPackages) {
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(
                org.apache.camel.component.rocketmq.RocketMQConsumer.class.getName()));
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(DefaultMQPushConsumer.class.getName()));

        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(SerializeConfig.class.getName()));
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(ParserConfig.class.getName()));
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(ASMClassLoader.class.getName()));

        // RocketMQ logging framework uses a shaded logback with AsyncAppender that creates threads.
        // Ensure all RocketMQ classes are runtime-initialized to prevent threads in the image heap.
        runtimeInitializedPackages.produce(new RuntimeInitializedPackageBuildItem("org.apache.rocketmq"));

        // Fastjson references optional types (e.g., javax.money.Monetary) that may not be on the classpath.
        // Runtime-initialize the package to avoid build-time linkage errors.
        runtimeInitializedPackages.produce(new RuntimeInitializedPackageBuildItem("com.alibaba.fastjson"));
    }
}
