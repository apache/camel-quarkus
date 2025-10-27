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
package org.apache.camel.quarkus.support.debezium.deployment;

import io.debezium.connector.common.BaseSourceTask;
import io.debezium.embedded.async.ConvertingAsyncEngineBuilderFactory;
import io.debezium.engine.DebeziumEngine;
import io.debezium.pipeline.notification.channels.LogNotificationChannel;
import io.debezium.pipeline.notification.channels.SinkNotificationChannel;
import io.debezium.pipeline.notification.channels.jmx.JmxNotificationChannel;
import io.debezium.pipeline.signal.actions.StandardActionProvider;
import io.debezium.pipeline.signal.channels.FileSignalChannel;
import io.debezium.pipeline.signal.channels.KafkaSignalChannel;
import io.debezium.pipeline.signal.channels.SourceSignalChannel;
import io.debezium.pipeline.signal.channels.jmx.JmxSignalChannel;
import io.debezium.pipeline.signal.channels.process.InProcessSignalChannel;
import io.debezium.pipeline.txmetadata.DefaultTransactionMetadataFactory;
import io.debezium.schema.SchemaTopicNamingStrategy;
import io.debezium.snapshot.lock.NoLockingSupport;
import io.debezium.snapshot.mode.AlwaysSnapshotter;
import io.debezium.snapshot.mode.ConfigurationBasedSnapshotter;
import io.debezium.snapshot.mode.InitialOnlySnapshotter;
import io.debezium.snapshot.mode.InitialSnapshotter;
import io.debezium.snapshot.mode.NeverSnapshotter;
import io.debezium.snapshot.mode.NoDataSnapshotter;
import io.debezium.snapshot.mode.RecoverySnapshotter;
import io.debezium.snapshot.mode.WhenNeededSnapshotter;
import io.debezium.snapshot.spi.SnapshotLock;
import io.debezium.storage.file.history.FileSchemaHistory;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import org.apache.kafka.common.security.authenticator.SaslClientAuthenticator;
import org.apache.kafka.connect.json.JsonConverter;
import org.apache.kafka.connect.source.SourceTask;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

public class DebeziumSupportProcessor {

    @BuildStep
    void addDependencies(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("org.apache.kafka", "connect-json"));
        indexDependency.produce(new IndexDependencyBuildItem("io.debezium", "debezium-api"));
    }

    @BuildStep
    RuntimeInitializedClassBuildItem runtimeInitializedClasses() {
        return new RuntimeInitializedClassBuildItem(SaslClientAuthenticator.class.getName());
    }

    @BuildStep
    void reflectiveClasses(CombinedIndexBuildItem combinedIndex, BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        IndexView index = combinedIndex.getIndex();

        String[] dtos = index.getKnownClasses().stream().map(ci -> ci.name().toString())
                .filter(n -> n.startsWith("org.apache.kafka.connect.json")
                        || n.startsWith("io.debezium.engine.spi"))
                .toArray(String[]::new);
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(dtos).fields().build());

        dtos = index.getAllKnownImplementations(DotName.createSimple(SnapshotLock.class.getName())).stream()
                .map(ci -> ci.name().toString())
                .toArray(String[]::new);
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(dtos).fields().build());

        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "org.apache.kafka.connect.storage.FileOffsetBackingStore",
                "org.apache.kafka.connect.storage.MemoryOffsetBackingStore",
                "io.debezium.storage.kafka.history.KafkaSchemaHistory",
                "io.debezium.relational.history.FileDatabaseHistory",
                "io.debezium.embedded.ConvertingEngineBuilderFactory",
                "io.debezium.processors.PostProcessorRegistry",
                "io.debezium.pipeline.txmetadata.DefaultTransactionMetadataFactory",
                "io.debezium.schema.SchemaTopicNamingStrategy",
                "io.debezium.storage.file.history.FileSchemaHistory")
                .build());

        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                DebeziumEngine.BuilderFactory.class,
                ConvertingAsyncEngineBuilderFactory.class,
                SaslClientAuthenticator.class,
                JsonConverter.class,
                DefaultTransactionMetadataFactory.class,
                SchemaTopicNamingStrategy.class,
                BaseSourceTask.class,
                SinkNotificationChannel.class,
                LogNotificationChannel.class,
                JmxNotificationChannel.class,
                SnapshotLock.class,
                NoLockingSupport.class,
                AlwaysSnapshotter.class,
                InitialSnapshotter.class,
                InitialOnlySnapshotter.class,
                NoDataSnapshotter.class,
                RecoverySnapshotter.class,
                WhenNeededSnapshotter.class,
                NeverSnapshotter.class,
                ConfigurationBasedSnapshotter.class,
                SourceSignalChannel.class,
                KafkaSignalChannel.class,
                FileSignalChannel.class,
                JmxSignalChannel.class,
                InProcessSignalChannel.class,
                StandardActionProvider.class,
                SourceTask.class,
                ConvertingAsyncEngineBuilderFactory.class,
                DefaultTransactionMetadataFactory.class,
                SchemaTopicNamingStrategy.class,
                FileSchemaHistory.class)
                .build());
    }

    @BuildStep
    void registerNativeImageResources(BuildProducer<NativeImageResourceBuildItem> resources) {
        resources.produce(new NativeImageResourceBuildItem(
                "META-INF/services/io.debezium.embedded.async.ConvertingAsyncEngineBuilderFactory"));
        resources.produce(
                new NativeImageResourceBuildItem("META-INF/services/io.debezium.engine.DebeziumEngine$BuilderFactory"));
        resources.produce(new NativeImageResourceBuildItem("META-INF/services/io.debezium.spi.snapshot.Snapshotter"));
        resources.produce(
                new NativeImageResourceBuildItem("META-INF/services/io.debezium.pipeline.signal.channels.SignalChannelReader"));
        resources.produce(new NativeImageResourceBuildItem(
                "META-INF/services/io.debezium.pipeline.notification.channels.NotificationChannel"));
        resources.produce(new NativeImageResourceBuildItem("META-INF/services/io.debezium.processors.PostProcessorProducer"));

        resources.produce(new NativeImageResourceBuildItem("META-INF/services/io.debezium.snapshot.spi.SnapshotLock"));
        resources.produce(new NativeImageResourceBuildItem("META-INF/services/io.debezium.snapshot.spi.SnapshotQuery"));
        resources
                .produce(new NativeImageResourceBuildItem("META-INF/services/org.apache.kafka.connect.source.SourceConnector"));
    }

}
