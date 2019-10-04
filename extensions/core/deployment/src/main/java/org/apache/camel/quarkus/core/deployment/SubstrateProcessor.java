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
package org.apache.camel.quarkus.core.deployment;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import javax.inject.Inject;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveMethodBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateConfigBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.Consumer;
import org.apache.camel.Converter;
import org.apache.camel.Endpoint;
import org.apache.camel.Producer;
import org.apache.camel.TypeConverter;
import org.apache.camel.quarkus.core.Flags;
import org.apache.camel.spi.ExchangeFormatter;
import org.apache.camel.spi.PropertiesComponent;
import org.apache.camel.spi.ScheduledPollConsumerScheduler;
import org.apache.camel.spi.StreamCachingStrategy;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SubstrateProcessor {
    /*
     * SubstrateVM configuration steps related to camel core.
     */
    public static class Core {
        private static final List<Class<?>> CAMEL_REFLECTIVE_CLASSES = Arrays.asList(
            Endpoint.class,
            Consumer.class,
            Producer.class,
            TypeConverter.class,
            ExchangeFormatter.class,
            ScheduledPollConsumerScheduler.class,
            Component.class,
            CamelContext.class,
            StreamCachingStrategy.class,
            StreamCachingStrategy.SpoolUsedHeapMemoryLimit.class,
            PropertiesComponent.class);

        @Inject
        BuildProducer<ReflectiveClassBuildItem> reflectiveClass;
        @Inject
        BuildProducer<ReflectiveMethodBuildItem> reflectiveMethod;
        @Inject
        BuildProducer<SubstrateResourceBuildItem> resource;
        @Inject
        ApplicationArchivesBuildItem applicationArchivesBuildItem;

        @BuildStep
        SubstrateConfigBuildItem cache() {
            return SubstrateConfigBuildItem.builder()
                // TODO: switch back to caffeine once https://github.com/apache/camel-quarkus/issues/80 gets fixed
                .addNativeImageSystemProperty("CamelWarmUpLRUCacheFactory", "true")
                .addNativeImageSystemProperty("CamelSimpleLRUCacheFactory", "true")
                .build();
        }

        @BuildStep
        void process(CombinedIndexBuildItem combinedIndex) {
            IndexView view = combinedIndex.getIndex();

            CAMEL_REFLECTIVE_CLASSES.stream()
                .map(Class::getName)
                .map(DotName::createSimple)
                .map(view::getAllKnownImplementors)
                .flatMap(Collection::stream)
                .filter(CamelSupport::isPublic)
                .forEach(v -> addReflectiveClass(true, v.name().toString()));

            Logger log = LoggerFactory.getLogger(SubstrateProcessor.class);
            DotName converter = DotName.createSimple(Converter.class.getName());
            List<ClassInfo> converterClasses = view.getAnnotations(converter)
                .stream()
                .filter(ai -> ai.target().kind() == Kind.CLASS)
                .filter(ai -> {
                    AnnotationValue av = ai.value("loader");
                    boolean isLoader = av != null && av.asBoolean();
                    // filter out camel-base converters which are automatically inlined in the CoreStaticTypeConverterLoader
                    // need to revisit with Camel 3.0.0-M3 which should improve this area
                    if (ai.target().asClass().name().toString().startsWith("org.apache.camel.converter.")) {
                        log.debug("Ignoring core " + ai + " " + ai.target().asClass().name());
                        return false;
                    } else if (isLoader) {
                        log.debug("Ignoring " + ai + " " + ai.target().asClass().name());
                        return false;
                    } else {
                        log.debug("Accepting " + ai + " " + ai.target().asClass().name());
                        return true;
                    }
                })
                .map(ai -> ai.target().asClass())
                .collect(Collectors.toList());

            log.debug("Converter classes: " + converterClasses);
            converterClasses.forEach(ci -> addReflectiveClass(false, ci.name().toString()));

            view.getAnnotations(converter)
                .stream()
                .filter(ai -> ai.target().kind() == Kind.METHOD)
                .filter(ai -> converterClasses.contains(ai.target().asMethod().declaringClass()))
                .map(ai -> ai.target().asMethod())
                .forEach(this::addReflectiveMethod);

            CamelSupport.resources(applicationArchivesBuildItem, "META-INF/maven/org.apache.camel/camel-core")
                .forEach(this::addResource);
            CamelSupport.resources(applicationArchivesBuildItem, CamelSupport.CAMEL_SERVICE_BASE_PATH)
                .forEach(this::addCamelService);
        }

        protected void addCamelService(Path p) {
            try (InputStream is = Files.newInputStream(p)) {
                Properties props = new Properties();
                props.load(is);
                for (Map.Entry<Object, Object> entry : props.entrySet()) {
                    String k = entry.getKey().toString();
                    if (k.equals("class")) {
                        addReflectiveClass(true, entry.getValue().toString());
                    } else if (k.endsWith(".class")) {
                        addReflectiveClass(true, entry.getValue().toString());
                        addResource(p);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        protected void addResource(Path p) {
            addResource(p.toString().substring(1));
        }

        protected void addResource(String r) {
            resource.produce(new SubstrateResourceBuildItem(r));
        }

        protected void addReflectiveClass(boolean methods, String... className) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(methods, false, className));
        }

        protected void addReflectiveMethod(MethodInfo mi) {
            reflectiveMethod.produce(new ReflectiveMethodBuildItem(mi));
        }
    }

    /*
     * SubstrateVM configuration steps related to camel main that are activated by default but can be
     * disabled by setting quarkus.camel.disable-main = true
     */
    public static class Main {
        @BuildStep(onlyIfNot = Flags.MainDisabled.class)
        void process(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

            IndexView view = combinedIndex.getIndex();

            //
            // Register routes as reflection aware as camel-main main use reflection
            // to bind beans to the registry
            //
            CamelSupport.getRouteBuilderClasses(view).forEach(name -> {
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, name));
            });

            reflectiveClass.produce(new ReflectiveClassBuildItem(
                true,
                false,
                org.apache.camel.main.DefaultConfigurationProperties.class,
                org.apache.camel.main.MainConfigurationProperties.class,
                org.apache.camel.main.HystrixConfigurationProperties.class,
                org.apache.camel.main.RestConfigurationProperties.class)
            );
        }
    }
}
