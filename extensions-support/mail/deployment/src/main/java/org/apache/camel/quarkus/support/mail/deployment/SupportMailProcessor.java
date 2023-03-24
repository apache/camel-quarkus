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
package org.apache.camel.quarkus.support.mail.deployment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.sun.mail.handlers.handler_base;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import jakarta.mail.Provider;
import org.jboss.jandex.DotName;

class SupportMailProcessor {

    @BuildStep
    void process(CombinedIndexBuildItem combinedIndex, BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageResourceBuildItem> resource,
            BuildProducer<ServiceProviderBuildItem> services) {
        List<String> providers = resources("META-INF/services/jakarta.mail.Provider")
                .flatMap(this::lines)
                .filter(s -> !s.startsWith("#"))
                .collect(Collectors.toList());

        List<String> streamProviders = resources("META-INF/services/jakarta.mail.util.StreamProvider")
                .flatMap(this::lines)
                .filter(s -> !s.startsWith("#"))
                .collect(Collectors.toList());

        List<String> imp1 = providers.stream()
                .map(this::loadClass)
                .map(this::instantiate)
                .map(Provider.class::cast)
                .map(Provider::getClassName)
                .collect(Collectors.toList());

        List<String> imp2 = Stream.of("META-INF/javamail.default.providers", "META-INF/javamail.providers")
                .flatMap(this::resources)
                .flatMap(this::lines)
                .filter(s -> !s.startsWith("#"))
                .flatMap(s -> Stream.of(s.split(";")))
                .map(String::trim)
                .filter(s -> s.startsWith("class="))
                .map(s -> s.substring("class=".length()))
                .collect(Collectors.toList());

        List<String> imp3 = resources("META-INF/mailcap")
                .flatMap(this::lines)
                .filter(s -> !s.startsWith("#"))
                .flatMap(s -> Stream.of(s.split(";")))
                .map(String::trim)
                .filter(s -> s.startsWith("x-java-content-handler="))
                .map(s -> s.substring("x-java-content-handler=".length()))
                .collect(Collectors.toList());

        reflectiveClass.produce(ReflectiveClassBuildItem.builder(Stream.concat(providers.stream(),
                Stream.concat(streamProviders.stream(),
                        Stream.concat(imp1.stream(), Stream.concat(imp2.stream(), imp3.stream()))))
                .distinct()
                .toArray(String[]::new)).build());

        resource.produce(new NativeImageResourceBuildItem(
                "META-INF/services/jakarta.mail.Provider",
                "META-INF/services/jakarta.mail.util.StreamProvider",
                "META-INF/javamail.charset.map",
                "META-INF/javamail.default.address.map",
                "META-INF/javamail.default.providers",
                "META-INF/javamail.address.map",
                "META-INF/javamail.providers",
                "META-INF/mailcap"));

        //jakarta activation spi
        combinedIndex.getIndex().getKnownClasses()
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .filter(name -> name.startsWith("jakarta.activation.spi."))
                .forEach(name -> combinedIndex.getIndex().getKnownDirectImplementors(DotName.createSimple(name))
                        .stream()
                        .forEach(service -> services.produce(
                                new ServiceProviderBuildItem(name, service.name().toString()))));
    }

    @BuildStep
    void registerDependencyForIndex(BuildProducer<IndexDependencyBuildItem> items) {
        items.produce(new IndexDependencyBuildItem("jakarta.activation", "jakarta.activation-api"));
        items.produce(new IndexDependencyBuildItem("org.eclipse.angus", "angus-activation"));
        items.produce(new IndexDependencyBuildItem("org.eclipse.angus", "angus-mail"));
    }

    @BuildStep
    void runtimeInitializedClasses(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {

        combinedIndex.getIndex()
                .getAllKnownSubclasses(DotName.createSimple(handler_base.class.getName()))
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeInitializedClass::produce);
    }

    private Stream<URL> resources(String path) {
        try {
            return enumerationAsStream(getClass().getClassLoader().getResources(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Class<?> loadClass(String name) {
        try {
            return getClass().getClassLoader().loadClass(name);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T instantiate(Class<T> clazz) {
        try {
            return clazz.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Stream<String> lines(URL url) {
        try (InputStream is = url.openStream()) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                return br.lines().collect(Collectors.toList()).stream();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private <T> Stream<T> enumerationAsStream(Enumeration<T> e) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        new Iterator<T>() {
                            public T next() {
                                return e.nextElement();
                            }

                            public boolean hasNext() {
                                return e.hasMoreElements();
                            }
                        },
                        Spliterator.ORDERED),
                false);
    }
}
