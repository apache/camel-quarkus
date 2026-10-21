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
package org.apache.camel.quarkus.component.management.deployment;

import java.io.NotSerializableException;
import java.io.ObjectStreamException;
import java.lang.reflect.Modifier;
import java.rmi.NotBoundException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.AbstractOwnableSynchronizer;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.management.MBeanException;
import javax.management.MBeanServerNotification;
import javax.management.ObjectInstance;
import javax.management.modelmbean.DescriptorSupport;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import javax.management.modelmbean.ModelMBeanOperationInfo;
import javax.management.modelmbean.RequiredModelMBean;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.NativeMonitoringBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.NativeConfig;
import jakarta.transaction.NotSupportedException;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.RuntimeExchangeException;
import org.apache.camel.api.management.ManagedAttribute;
import org.apache.camel.api.management.ManagedNotification;
import org.apache.camel.api.management.ManagedNotifications;
import org.apache.camel.api.management.ManagedOperation;
import org.apache.camel.api.management.ManagedResource;
import org.apache.camel.api.management.mbean.ManagedStepMBean;
import org.apache.camel.quarkus.component.management.CamelManagementRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelSerializationBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.RuntimeCamelContextCustomizerBuildItem;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class ManagementProcessor {
    private static final String FEATURE = "camel-management";
    private static final String[] MANAGEMENT_NAME_PATTERN_PROPERTIES = {
            "camel.main.jmxManagementNamePattern",
            "camel.main.jmx-management-name-pattern"
    };
    private static final String AUXILIARY_APPLICATION_MANAGEMENT_NAME_PATTERN = "#name#-test";
    private static final Class<?>[] CAMEL_MANAGEMENT_ANNOTATIONS = {
            ManagedAttribute.class,
            ManagedNotification.class,
            ManagedNotifications.class,
            ManagedOperation.class,
            ManagedResource.class
    };
    private static final String[] SERIALIZATION_CLASSES = {
            AbstractOwnableSynchronizer.class.getName(),
            AbstractQueuedSynchronizer.class.getName(),
            "java.util.concurrent.ConcurrentHashMap$CollectionView",
            ConcurrentHashMap.class.getName(),
            "java.util.concurrent.ConcurrentHashMap$KeySetView",
            "java.util.concurrent.ConcurrentHashMap$Segment",
            DescriptorSupport.class.getName(),
            ModelMBeanAttributeInfo.class.getName(),
            ModelMBeanInfoSupport.class.getName(),
            ModelMBeanOperationInfo.class.getName(),
            MBeanException.class.getName(),
            MBeanServerNotification.class.getName(),
            NoSuchMethodException.class.getName(),
            NotBoundException.class.getName(),
            NotSerializableException.class.getName(),
            NotSupportedException.class.getName(),
            Object.class.getName(),
            ObjectInstance.class.getName(),
            ObjectStreamException.class.getName(),
            ReentrantLock.class.getName(),
            "java.util.concurrent.locks.ReentrantLock$NonfairSync",
            "java.util.concurrent.locks.ReentrantLock$Sync",
            RuntimeCamelException.class.getName(),
            RuntimeExchangeException.class.getName(),
            "java.util.Collections$UnmodifiableSet",
    };

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void camelManagementMBeanSupport(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageProxyDefinitionBuildItem> nativeImageProxy,
            BuildProducer<CamelSerializationBuildItem> camelSerialization) {

        IndexView index = combinedIndex.getIndex();

        // Find Camel management interfaces and configure native proxy definitions for them
        Set<String> managedBeanInterfaces = getManagedTypes(index, classInfo -> Modifier.isInterface(classInfo.flags()));
        // ManagedStepMBean doesn't have any annotation
        managedBeanInterfaces.add(ManagedStepMBean.class.getName());
        managedBeanInterfaces.stream()
                .map(NativeImageProxyDefinitionBuildItem::new)
                .forEach(nativeImageProxy::produce);

        // Find the implementations of the managed bean interfaces and register them for reflection
        Set<String> managedBeanClasses = managedBeanInterfaces.stream()
                .map(DotName::createSimple)
                .flatMap((dotName) -> index.getAllKnownImplementations(dotName).stream())
                .map(classInfo -> classInfo.name().toString())
                .collect(Collectors.toSet());

        // Get any managed classes (ones that do not implement Camel managed interfaces) and register them for reflection
        managedBeanClasses.addAll(getManagedTypes(index, classInfo -> !Modifier.isInterface(classInfo.flags())));
        // DefaultManagementMBeanAssembler dynamically instantiates this via the MBeanServer
        managedBeanClasses.add(RequiredModelMBean.class.getName());

        // Managed operations & attributes may be inherited from superclasses that have no management annotations
        managedBeanClasses.addAll(getSuperClasses(index, managedBeanClasses));

        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder(managedBeanClasses.toArray(String[]::new))
                        .fields().methods().build());

        // Add serialization support for some core JDK & Camel classes, plus the javax.management classes that JMX requires
        camelSerialization.produce(new CamelSerializationBuildItem(SERIALIZATION_CLASSES));
    }

    private static Set<String> getSuperClasses(IndexView index, Set<String> classNames) {
        Set<String> superClasses = new HashSet<>();
        for (String className : classNames) {
            ClassInfo classInfo = index.getClassByName(className);
            while (classInfo != null && classInfo.superName() != null
                    && !classInfo.superName().equals(DotName.OBJECT_NAME)) {
                superClasses.add(classInfo.superName().toString());
                classInfo = index.getClassByName(classInfo.superName());
            }
        }
        return superClasses;
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    RuntimeCamelContextCustomizerBuildItem configureCamelContext(CamelManagementRecorder recorder) {
        return new RuntimeCamelContextCustomizerBuildItem(recorder.createContextCustomizer());
    }

    @BuildStep
    void auxiliaryApplicationManagementName(
            LaunchModeBuildItem launchMode,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> runTimeConfigurationDefault) {
        // An auxiliary application, such as the dev mode continuous testing application, shares the MBeanServer with
        // the primary application. Use a distinct management name so that the CamelContext MBean names do not clash
        if (launchMode.isAuxiliaryApplication()) {
            boolean configured = Stream.of(MANAGEMENT_NAME_PATTERN_PROPERTIES)
                    .anyMatch(name -> ConfigProvider.getConfig().getOptionalValue(name, String.class).isPresent());
            if (!configured) {
                runTimeConfigurationDefault.produce(new RunTimeConfigurationDefaultBuildItem(
                        MANAGEMENT_NAME_PATTERN_PROPERTIES[0], AUXILIARY_APPLICATION_MANAGEMENT_NAME_PATTERN));
            }
        }
    }

    @BuildStep
    void enableNativeMonitoring(BuildProducer<NativeMonitoringBuildItem> nativeMonitoring) {
        nativeMonitoring.produce(new NativeMonitoringBuildItem(NativeConfig.MonitoringOption.JMXSERVER));
        boolean debugEnabled = ConfigProvider.getConfig().getOptionalValue("camel.debug.enabled", boolean.class).orElse(false);
        if (debugEnabled) {
            nativeMonitoring.produce(new NativeMonitoringBuildItem(NativeConfig.MonitoringOption.JMXCLIENT));
        }
    }

    private Set<String> getManagedTypes(IndexView index, Predicate<ClassInfo> typeFilter) {
        return Stream.of(CAMEL_MANAGEMENT_ANNOTATIONS)
                .flatMap(annotation -> index.getAnnotations(annotation).stream())
                .map(AnnotationInstance::target)
                .map(annotationTarget -> {
                    Kind kind = annotationTarget.kind();
                    if (kind.equals(Kind.CLASS)) {
                        return annotationTarget.asClass();
                    } else if (kind.equals(Kind.FIELD)) {
                        return annotationTarget.asField().declaringClass();
                    } else if (kind.equals(Kind.METHOD)) {
                        return annotationTarget.asMethod().declaringClass();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .filter(typeFilter)
                .map(classInfo -> classInfo.name().toString())
                .collect(Collectors.toSet());
    }
}
