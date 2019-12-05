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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.Consumer;
import org.apache.camel.Converter;
import org.apache.camel.Endpoint;
import org.apache.camel.Producer;
import org.apache.camel.TypeConverter;
import org.apache.camel.quarkus.core.Flags;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.ExchangeFormatter;
import org.apache.camel.spi.PropertiesComponent;
import org.apache.camel.spi.ScheduledPollConsumerScheduler;
import org.apache.camel.spi.StreamCachingStrategy;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class NativeImageProcessor {
    /*
     * NativeImage configuration steps related to camel core.
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
                PropertiesComponent.class,
                DataFormat.class);

        @BuildStep
        void reflectiveItems(
                CombinedIndexBuildItem combinedIndex,
                BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
                BuildProducer<ReflectiveMethodBuildItem> reflectiveMethod) {

            final IndexView view = combinedIndex.getIndex();

            CAMEL_REFLECTIVE_CLASSES.stream()
                    .map(Class::getName)
                    .map(DotName::createSimple)
                    .map(view::getAllKnownImplementors)
                    .flatMap(Collection::stream)
                    .filter(CamelSupport::isPublic)
                    .forEach(v -> reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, v.name().toString())));

            Logger log = LoggerFactory.getLogger(NativeImageProcessor.class);
            DotName converter = DotName.createSimple(Converter.class.getName());
            List<ClassInfo> converterClasses = view.getAnnotations(converter)
                    .stream()
                    .filter(ai -> ai.target().kind() == Kind.CLASS)
                    .filter(ai -> {
                        AnnotationValue av = ai.value("loader");
                        boolean isLoader = av != null && av.asBoolean();
                        // filter out camel-base converters which are automatically inlined in the
                        // CoreStaticTypeConverterLoader
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
            converterClasses
                    .forEach(ci -> reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, ci.name().toString())));

            view.getAnnotations(converter)
                    .stream()
                    .filter(ai -> ai.target().kind() == Kind.METHOD)
                    .filter(ai -> converterClasses.contains(ai.target().asMethod().declaringClass()))
                    .map(ai -> ai.target().asMethod())
                    .forEach(mi -> reflectiveMethod.produce(new ReflectiveMethodBuildItem(mi)));

        }

        @BuildStep
        void resources(
                ApplicationArchivesBuildItem applicationArchivesBuildItem,
                BuildProducer<NativeImageResourceBuildItem> resource) {

            CamelSupport.resources(applicationArchivesBuildItem, "META-INF/maven/org.apache.camel/camel-base")
                    .forEach(p -> resource.produce(new NativeImageResourceBuildItem(p.toString().substring(1))));
        }

        @BuildStep
        void camelServices(
                List<CamelServiceBuildItem> camelServices,
                BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

            camelServices.stream()
                    .forEach(service -> {
                        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, service.type));
                    });

        }

    }

    /*
     * NativeImage configuration steps related to camel main that are activated by default but can be
     * disabled by setting quarkus.camel.main.enabled = false
     */
    public static class Main {
        @BuildStep(onlyIf = Flags.MainEnabled.class)
        void process(
                List<CamelRoutesBuilderClassBuildItem> camelRoutesBuilders,
                BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

            //
            // Register routes as reflection aware as camel-main main use reflection
            // to bind beans to the registry
            //
            camelRoutesBuilders.forEach(dotName -> {
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, dotName.toString()));
            });

            reflectiveClass.produce(new ReflectiveClassBuildItem(
                    true,
                    false,
                    org.apache.camel.main.DefaultConfigurationProperties.class,
                    org.apache.camel.main.MainConfigurationProperties.class,
                    org.apache.camel.main.HystrixConfigurationProperties.class,
                    org.apache.camel.main.RestConfigurationProperties.class));
        }
    }
}
