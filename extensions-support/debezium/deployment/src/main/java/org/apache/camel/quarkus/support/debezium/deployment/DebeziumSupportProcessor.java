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

import java.util.function.BooleanSupplier;

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
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.gizmo.Gizmo;
import org.apache.camel.quarkus.support.debezium.DebeziumComponentObserver;
import org.apache.kafka.common.security.authenticator.SaslClientAuthenticator;
import org.apache.kafka.connect.json.JsonConverter;
import org.apache.kafka.connect.source.SourceTask;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class DebeziumSupportProcessor {

    private static final Logger LOG = Logger.getLogger(DebeziumSupportProcessor.class);

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
    public void configureKafkaComponentForDevServices(BuildProducer<AdditionalBeanBuildItem> additionalBean) {
        additionalBean.produce(AdditionalBeanBuildItem.unremovableOf(DebeziumComponentObserver.class));
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

    // TODO: Remove this - https://github.com/apache/camel-quarkus/issues/8530
    @BuildStep(onlyIf = KafkaClients42IsPresent.class)
    BytecodeTransformerBuildItem patchConfigInfos() {
        // Patch ConfigInfos to add values() method as duplicate of configs()
        // This provides backward compatibility for Debezium with kafka-clients 4.2.0
        return new BytecodeTransformerBuildItem.Builder()
                .setClassToTransform("org.apache.kafka.connect.runtime.rest.entities.ConfigInfos")
                .setCacheable(true)
                .setVisitorFunction((className, classVisitor) -> new ConfigInfosClassVisitor(classVisitor))
                .build();
    }

    // TODO: Remove this - https://github.com/apache/camel-quarkus/issues/8530
    static final class KafkaClients42IsPresent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            try {
                // Check if ConfigInfos.values() is present. If it's not, then kafka-clients >= 4.2.0 is on the classpath
                Class<?> configInfos = Thread.currentThread().getContextClassLoader()
                        .loadClass("org.apache.kafka.connect.runtime.rest.entities.ConfigInfos");
                configInfos.getDeclaredMethod("values");
                return false;
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                return true;
            }
        }
    }

    /**
     * Adds a values() method to ConfigInfos that duplicates the configs() method.
     * This provides backward compatibility with older Debezium versions.
     */
    static class ConfigInfosClassVisitor extends ClassVisitor {

        private String configsFieldDescriptor = null;
        private String configsMethodSignature = null;

        protected ConfigInfosClassVisitor(ClassVisitor classVisitor) {
            super(Gizmo.ASM_API_VERSION, classVisitor);
        }

        @Override
        public org.objectweb.asm.FieldVisitor visitField(int access, String name, String descriptor, String signature,
                Object value) {
            // Track the configs field descriptor
            if ("configs".equals(name)) {
                configsFieldDescriptor = descriptor;
            }
            return super.visitField(access, name, descriptor, signature, value);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                String[] exceptions) {
            // Track the signature of configs() method
            if ("configs".equals(name) && "()Ljava/util/List;".equals(descriptor)) {
                configsMethodSignature = signature;
            }
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        @Override
        public void visitEnd() {
            // Add values() method that duplicates configs()
            LOG.debug("Adding values() method to ConfigInfos as duplicate of configs()");

            MethodVisitor mv = cv.visitMethod(
                    Opcodes.ACC_PUBLIC,
                    "values",
                    "()Ljava/util/List;",
                    configsMethodSignature, // Same generic signature as configs()
                    null);

            if (mv != null) {
                mv.visitCode();
                // Method body: return this.configs;
                mv.visitVarInsn(Opcodes.ALOAD, 0); // Load 'this'
                mv.visitFieldInsn(Opcodes.GETFIELD,
                        "org/apache/kafka/connect/runtime/rest/entities/ConfigInfos",
                        "configs",
                        configsFieldDescriptor != null ? configsFieldDescriptor : "Ljava/util/List;");
                mv.visitInsn(Opcodes.ARETURN); // Return the field value
                mv.visitMaxs(1, 1); // Max stack=1, max locals=1
                mv.visitEnd();
            }

            super.visitEnd();
        }
    }
}
