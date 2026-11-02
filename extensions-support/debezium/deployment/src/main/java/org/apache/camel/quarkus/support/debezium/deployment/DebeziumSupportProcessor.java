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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.debezium.connector.SourceInfoStructMaker;
import io.debezium.engine.DebeziumEngine;
import io.debezium.pipeline.txmetadata.spi.TransactionMetadataFactory;
import io.debezium.processors.PostProcessorRegistry;
import io.debezium.processors.reselect.cache.ReselectColumnCache;
import io.debezium.relational.TableMappingStorage;
import io.debezium.relational.history.SchemaHistory;
import io.debezium.snapshot.spi.SnapshotLock;
import io.debezium.spi.topic.TopicNamingStrategy;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathFilter;
import org.apache.camel.quarkus.core.deployment.spi.CamelSerializationBuildItem;
import org.apache.camel.quarkus.support.debezium.DebeziumComponentObserver;
import org.apache.kafka.connect.source.SourceTask;
import org.apache.kafka.connect.storage.Converter;
import org.apache.kafka.connect.storage.FileOffsetBackingStore;
import org.apache.kafka.connect.storage.HeaderConverter;
import org.apache.kafka.connect.storage.KafkaOffsetBackingStore;
import org.apache.kafka.connect.storage.MemoryOffsetBackingStore;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

public class DebeziumSupportProcessor {
    private static final String DEBEZIUM_GROUP_ID = "io.debezium";
    private static final String SERVICES_PATH = "META-INF/services/";
    // Holds no plugin implementations and would add thousands of ANTLR classes to the index
    private static final Set<String> UNINDEXED_ARTIFACTS = Set.of("debezium-ddl-parser");

    // Types whose implementations Debezium instantiates from class names held in connector configuration
    static final List<Class<?>> CONFIGURABLE_PLUGIN_TYPES = List.of(
            ReselectColumnCache.class,
            SchemaHistory.class,
            SourceInfoStructMaker.class,
            SourceTask.class,
            TableMappingStorage.class,
            TopicNamingStrategy.class,
            TransactionMetadataFactory.class);

    // Classes named by configuration defaults that are registered for reflection individually
    static final List<Class<?>> REFLECTIVE_CLASSES = List.of(
            FileOffsetBackingStore.class,
            MemoryOffsetBackingStore.class,
            KafkaOffsetBackingStore.class,
            DebeziumEngine.BuilderFactory.class,
            SnapshotLock.class,
            SourceTask.class);

    // Services registered whether or not a Debezium artifact declares them
    static final List<Class<?>> REQUIRED_SERVICES = List.of(Converter.class, HeaderConverter.class);

    // Services declared by Debezium artifacts that the embedded engine never loads through ServiceLoader
    static final Set<String> EXCLUDED_SERVICES = Set.of(
            "io.debezium.metadata.ComponentMetadataProvider",
            "org.apache.kafka.connect.sink.SinkConnector",
            "org.apache.kafka.connect.transforms.Transformation",
            "org.hibernate.boot.spi.MetadataSourcesContributor",
            "org.infinispan.protostream.SerializationContextInitializer");

    @BuildStep
    void addDependencies(CurateOutcomeBuildItem curateOutcome, BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("org.apache.kafka", "connect-json"));
        debeziumDependencies(curateOutcome).stream()
                .filter(dependency -> !UNINDEXED_ARTIFACTS.contains(dependency.getArtifactId()))
                .forEach(dependency -> indexDependency
                        .produce(new IndexDependencyBuildItem(dependency.getGroupId(), dependency.getArtifactId())));
    }

    @BuildStep
    public void configureKafkaComponentForDevServices(BuildProducer<AdditionalBeanBuildItem> additionalBean) {
        additionalBean.produce(AdditionalBeanBuildItem.unremovableOf(DebeziumComponentObserver.class));
    }

    @BuildStep
    CamelSerializationBuildItem serialization() {
        // FileOffsetBackingStore persists offsets with Java serialization
        return new CamelSerializationBuildItem();
    }

    @BuildStep
    void reflectiveClasses(CombinedIndexBuildItem combinedIndex, BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        IndexView index = combinedIndex.getIndex();

        String[] debeziumEngineSpiClasses = index.getKnownClasses().stream().map(ci -> ci.name().toString())
                .filter(n -> n.startsWith("io.debezium.engine.spi"))
                .toArray(String[]::new);
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(debeziumEngineSpiClasses).fields().build());

        String[] jsonConverters = index.getKnownClasses().stream().map(ci -> ci.name().toString())
                .filter(n -> n.startsWith("org.apache.kafka.connect.json"))
                .toArray(String[]::new);
        reflectiveClasses.produce(
                ReflectiveClassBuildItem.builder(jsonConverters).fields().constructors().methods().build());

        String[] dtos = index.getAllKnownImplementations(DotName.createSimple(SnapshotLock.class.getName())).stream()
                .map(ci -> ci.name().toString())
                .toArray(String[]::new);
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(dtos).fields().build());

        String[] plugins = CONFIGURABLE_PLUGIN_TYPES.stream()
                .flatMap(type -> {
                    DotName name = DotName.createSimple(type);
                    return type.isInterface()
                            ? index.getAllKnownImplementations(name).stream()
                            : index.getAllKnownSubclasses(name).stream();
                })
                .filter(ci -> !ci.isInterface())
                .map(ClassInfo::name)
                .map(DotName::toString)
                .toArray(String[]::new);
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(plugins).build());

        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(REFLECTIVE_CLASSES.toArray(Class<?>[]::new)).build());

        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(PostProcessorRegistry.class)
                .methods()
                .build());
    }

    @BuildStep
    void registerServiceProviders(
            CurateOutcomeBuildItem curateOutcome,
            BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        Set<String> services = new HashSet<>();
        REQUIRED_SERVICES.forEach(service -> services.add(service.getName()));
        PathFilter pathFilter = PathFilter.forIncludes(Set.of(SERVICES_PATH + "*"));
        debeziumDependencies(curateOutcome)
                .forEach(dependency -> dependency.getContentTree(pathFilter).walk(visit -> {
                    String resourceName = visit.getResourceName();
                    if (resourceName.length() > SERVICES_PATH.length()) {
                        services.add(resourceName.substring(SERVICES_PATH.length()));
                    }
                }));

        services.stream()
                .filter(service -> !EXCLUDED_SERVICES.contains(service))
                .map(ServiceProviderBuildItem::allProvidersFromClassPath)
                .forEach(serviceProvider::produce);
    }

    private static List<ResolvedDependency> debeziumDependencies(CurateOutcomeBuildItem curateOutcome) {
        return curateOutcome.getApplicationModel()
                .getRuntimeDependencies()
                .stream()
                .filter(dependency -> dependency.getGroupId().equals(DEBEZIUM_GROUP_ID))
                .toList();
    }
}
