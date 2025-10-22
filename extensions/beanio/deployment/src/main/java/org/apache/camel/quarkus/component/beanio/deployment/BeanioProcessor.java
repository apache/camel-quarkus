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
package org.apache.camel.quarkus.component.beanio.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.beanio.BeanReaderErrorHandler;
import org.beanio.annotation.Record;
import org.beanio.stream.RecordParserFactory;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class BeanioProcessor {
    private static final String FEATURE = "camel-beanio";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    IndexDependencyBuildItem indexDependencies() {
        return new IndexDependencyBuildItem("com.github.beanio", "beanio");
    }

    @BuildStep
    BeanioPropertiesBuildItem beanioProperties() {
        try {
            Properties properties = new Properties();
            try (InputStream in = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream("org/beanio/internal/config/beanio.properties")) {
                properties.load(in);
            }
            return new BeanioPropertiesBuildItem(properties);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @BuildStep
    void nativeImageResources(BuildProducer<NativeImageResourceBuildItem> nativeImageResource) {
        nativeImageResource.produce(new NativeImageResourceBuildItem("org/beanio/internal/config/beanio.properties"));
        nativeImageResource.produce(new NativeImageResourceBuildItem("beanio.properties"));
    }

    @BuildStep
    void registerForReflection(
            BeanioPropertiesBuildItem beanioProperties,
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        Properties properties = beanioProperties.getProperties();

        Set<String> handlersAndFactories = properties.keySet()
                .stream()
                .filter(key -> key.toString().contains("Factory") || key.toString().contains("Handler"))
                .map(properties::get)
                .map(Object::toString)
                .collect(Collectors.toUnmodifiableSet());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(handlersAndFactories.toArray(new String[0])).build());

        IndexView index = combinedIndex.getIndex();
        Set<String> recordParsers = index.getAllKnownImplementations(RecordParserFactory.class)
                .stream()
                .map(ClassInfo::name)
                .map(DotName::toString)
                .collect(Collectors.toUnmodifiableSet());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(recordParsers.toArray(new String[0])).methods(true).build());

        Set<String> parserConfiguration = index.getKnownClasses()
                .stream()
                .map(ClassInfo::name)
                .map(DotName::toString)
                .filter(name -> name.startsWith("org.beanio") && name.endsWith("ParserConfiguration"))
                .collect(Collectors.toUnmodifiableSet());
        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder(parserConfiguration.toArray(new String[0])).methods(true).build());

        Set<String> errorHandlers = index.getAllKnownImplementations(BeanReaderErrorHandler.class)
                .stream()
                .map(ClassInfo::name)
                .map(DotName::toString)
                .collect(Collectors.toUnmodifiableSet());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(errorHandlers.toArray(new String[0])).build());

        Set<String> recordClasses = index.getAnnotations(Record.class)
                .stream()
                .map(AnnotationInstance::target)
                .filter(target -> target.kind().equals(Kind.CLASS))
                .map(AnnotationTarget::asClass)
                .map(ClassInfo::name)
                .map(DotName::toString)
                .collect(Collectors.toUnmodifiableSet());
        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder(recordClasses.toArray(new String[0])).fields(true).methods(true)
                        .build());
    }

    @BuildStep
    void registerResourceBundles(BeanioPropertiesBuildItem beanioProperties,
            BuildProducer<NativeImageResourceBundleBuildItem> nativeImageResourceBundle) {
        Properties properties = beanioProperties.getProperties();
        properties.keySet()
                .stream()
                .filter(key -> key.toString().endsWith(".messages"))
                .map(properties::get)
                .map(Object::toString)
                .map(NativeImageResourceBundleBuildItem::new)
                .forEach(nativeImageResourceBundle::produce);
    }
}
