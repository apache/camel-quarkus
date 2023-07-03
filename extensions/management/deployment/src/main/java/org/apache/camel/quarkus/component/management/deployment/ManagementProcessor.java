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

import java.lang.reflect.Modifier;
import java.rmi.NotBoundException;
import java.util.Objects;
import java.util.Set;
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
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.api.management.ManagedAttribute;
import org.apache.camel.api.management.ManagedNotification;
import org.apache.camel.api.management.ManagedNotifications;
import org.apache.camel.api.management.ManagedOperation;
import org.apache.camel.api.management.ManagedResource;
import org.apache.camel.quarkus.component.management.CamelManagementRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelSerializationBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.RuntimeCamelContextCustomizerBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class ManagementProcessor {
    private static final String FEATURE = "camel-management";
    private static final Class<?>[] CAMEL_MANAGEMENT_ANNOTATIONS = {
            ManagedAttribute.class,
            ManagedNotification.class,
            ManagedNotifications.class,
            ManagedOperation.class,
            ManagedResource.class
    };
    private static final String[] SERIALIZATION_CLASSES = {
            DescriptorSupport.class.getName(),
            ModelMBeanAttributeInfo.class.getName(),
            ModelMBeanInfoSupport.class.getName(),
            ModelMBeanOperationInfo.class.getName(),
            MBeanException.class.getName(),
            MBeanServerNotification.class.getName(),
            NotBoundException.class.getName(),
            Object.class.getName(),
            ObjectInstance.class.getName(),
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
        managedBeanInterfaces.stream()
                .map(NativeImageProxyDefinitionBuildItem::new)
                .forEach(nativeImageProxy::produce);

        // Find the implementations of the managed bean interfaces and register them for reflection
        Set<String> managedBeanClasses = managedBeanInterfaces.stream()
                .map(DotName::createSimple)
                .flatMap((dotName) -> index.getAllKnownImplementors(dotName).stream())
                .map(classInfo -> classInfo.name().toString())
                .collect(Collectors.toSet());

        // Get any managed classes (ones that do not implement Camel managed interfaces) and register them for reflection
        managedBeanClasses.addAll(getManagedTypes(index, classInfo -> !Modifier.isInterface(classInfo.flags())));
        // DefaultManagementMBeanAssembler dynamically instantiates this via the MBeanServer
        managedBeanClasses.add(RequiredModelMBean.class.getName());

        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder(managedBeanClasses.toArray(String[]::new))
                        .fields().methods().build());

        // Various javax.management classes require serialization
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(SERIALIZATION_CLASSES).serialization().build());

        // Add serialization support for some core JDK & Camel classes
        camelSerialization.produce(new CamelSerializationBuildItem());
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    RuntimeCamelContextCustomizerBuildItem configureCamelContext(CamelManagementRecorder recorder) {
        return new RuntimeCamelContextCustomizerBuildItem(recorder.createContextCustomizer());
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
