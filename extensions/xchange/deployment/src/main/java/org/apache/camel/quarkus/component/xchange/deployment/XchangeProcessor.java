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
package org.apache.camel.quarkus.component.xchange.deployment;

import java.lang.reflect.Modifier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.ResolvedDependency;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.dto.Order;

class XchangeProcessor {

    private static final String FEATURE = "camel-xchange";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    void indexDependenciesAndResources(
            BuildProducer<IndexDependencyBuildItem> indexedDependency,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResource,
            CurateOutcomeBuildItem curateOutcome) {

        ApplicationModel applicationModel = curateOutcome.getApplicationModel();
        for (ResolvedDependency dependency : applicationModel.getDependencies()) {
            if (dependency.getGroupId().equals("org.knowm.xchange")) {
                // Index any org.knowm.xchange dependencies present on the classpath as they contain the APIs for interacting with each crypto exchange
                String artifactId = dependency.getArtifactId();
                indexedDependency.produce(new IndexDependencyBuildItem(dependency.getGroupId(), artifactId));

                // Include crypto exchange metadata resources
                String[] split = artifactId.split("-");
                if (split.length > 1) {
                    String cryptoExchange = split[split.length - 1];
                    nativeImageResource.produce(new NativeImageResourceBuildItem(cryptoExchange + ".json"));
                }
            }
        }

        indexedDependency.produce(new IndexDependencyBuildItem("jakarta.ws.rs", "jakarta.ws.rs-api"));
    }

    @BuildStep
    void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, CombinedIndexBuildItem combinedIndex) {
        // The xchange component dynamically instantiates Exchange classes so they must be registered for reflection
        IndexView index = combinedIndex.getIndex();
        String[] xchangeClasses = index.getAllKnownSubclasses(DotName.createSimple(BaseExchange.class.getName()))
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .toArray(String[]::new);
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(xchangeClasses).build());

        // DTO classes need to be serialized / deserialized
        final Pattern pattern = Pattern.compile("^org\\.knowm\\.xchange.*dto.*");
        String[] dtoClasses = index.getKnownClasses()
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .filter(className -> pattern.matcher(className).matches())
                .toArray(String[]::new);
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(dtoClasses).methods().fields().build());

        // rescu REST framework needs reflective access to the value method on some JAX-RS annotations
        String[] jaxrsAnnotations = index.getKnownClasses()
                .stream()
                .filter(ClassInfo::isAnnotation)
                .filter(classInfo -> classInfo.firstMethod("value") != null)
                .map(classInfo -> classInfo.name().toString())
                .filter(className -> className.startsWith("jakarta.ws.rs"))
                .toArray(String[]::new);
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(jaxrsAnnotations).methods().build());
    }

    @BuildStep
    void registerProxyClasses(BuildProducer<NativeImageProxyDefinitionBuildItem> nativeImageProxy,
            CombinedIndexBuildItem combinedIndexBuildItem) {
        // Some xchange libraries use JAX-RS proxies to interact with the exchange APIs so we need to register them
        IndexView index = combinedIndexBuildItem.getIndex();
        index.getAnnotations(DotName.createSimple("jakarta.ws.rs.Path"))
                .stream()
                .map(AnnotationInstance::target)
                .filter(target -> target.kind().equals(AnnotationTarget.Kind.CLASS))
                .map(AnnotationTarget::asClass)
                .filter(classInfo -> Modifier.isInterface(classInfo.flags()))
                .map(classInfo -> classInfo.name().toString())
                .filter(className -> className.startsWith("org.knowm.xchange"))
                .map(NativeImageProxyDefinitionBuildItem::new)
                .forEach(nativeImageProxy::produce);
    }

    @BuildStep
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses) {
        Stream.of(Order.class.getName())
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeInitializedClasses::produce);
    }

    @BuildStep
    void registerResourceBundles(BuildProducer<NativeImageResourceBundleBuildItem> producer) {
        producer.produce(new NativeImageResourceBundleBuildItem("sun.util.resources.CurrencyNames", "java.base"));
    }

}
