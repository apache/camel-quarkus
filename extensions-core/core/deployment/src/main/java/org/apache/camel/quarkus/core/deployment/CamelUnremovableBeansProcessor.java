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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import org.apache.camel.quarkus.core.deployment.catalog.BuildTimeCamelCatalog;
import org.apache.camel.quarkus.core.deployment.catalog.BuildTimeJsonSchemaResolver;
import org.apache.camel.quarkus.core.deployment.catalog.SchemaResource;
import org.apache.camel.quarkus.core.deployment.spi.BuildTimeCamelCatalogBuildItem;
import org.apache.camel.spi.InterceptStrategy;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.spi.annotations.Dataformat;
import org.apache.camel.spi.annotations.Language;
import org.apache.camel.tooling.model.BaseOptionModel;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CamelUnremovableBeansProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CamelProcessor.class);

    private static final DotName[] CATALOG_SCHEMA_TYPES = {
            DotName.createSimple(Component.class.getName()),
            DotName.createSimple(Dataformat.class.getName()),
            DotName.createSimple(Language.class.getName())
    };

    private static final DotName[] OPTIONAL_SERVICE_TYPES = {
            DotName.createSimple(InterceptStrategy.class.getName())
    };

    @BuildStep
    BuildTimeCamelCatalogBuildItem buildTimeCamelCatalog(CombinedIndexBuildItem combinedIndex) {
        Set<SchemaResource> resources = new HashSet<>();
        IndexView index = combinedIndex.getIndex();

        List<AnnotationInstance> annotations = Stream.of(CATALOG_SCHEMA_TYPES)
                .map(index::getAnnotations)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        for (AnnotationInstance instance : annotations) {
            SchemaResource resource = new SchemaResource();
            resource.setName(instance.value().asString());
            resource.setType(instance.name().withoutPackagePrefix().toLowerCase());
            resource.setClassName(instance.target().asClass().name());
            resources.add(resource);
        }

        BuildTimeJsonSchemaResolver resolver = new BuildTimeJsonSchemaResolver(resources);
        BuildTimeCamelCatalog catalog = new BuildTimeCamelCatalog(resolver);
        return new BuildTimeCamelCatalogBuildItem(catalog);
    }

    @BuildStep
    UnremovableBeanBuildItem unremovableCamelBeans(BuildTimeCamelCatalogBuildItem camelCatalogBuildItem) {
        BuildTimeCamelCatalog catalog = camelCatalogBuildItem.getCatalog();
        Set<DotName> unremovableClasses = catalog.getAllOptions()
                .stream()
                .filter(option -> option.getType().equals("object"))
                .filter(option -> !option.getJavaType().startsWith("java.lang"))
                .map(BaseOptionModel::getJavaType)
                .map(DotName::createSimple)
                .collect(Collectors.toSet());

        if (LOGGER.isDebugEnabled()) {
            unremovableClasses.stream().forEach(
                    unremovableClass -> LOGGER.debug("Registering camel unremovable bean class: {}", unremovableClass));
        }

        return UnremovableBeanBuildItem.beanTypes(unremovableClasses);
    }

    @BuildStep
    UnremovableBeanBuildItem unremovableOptionalServices(CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        Set<DotName> unremovableClasses = Stream.of(OPTIONAL_SERVICE_TYPES)
                .map(index::getAllKnownImplementors)
                .flatMap(Collection::stream)
                .map(ClassInfo::name)
                .collect(Collectors.toSet());

        if (LOGGER.isDebugEnabled()) {
            unremovableClasses.stream().forEach(
                    unremovableClass -> LOGGER.debug("Registering optional service unremovable bean class: {}",
                            unremovableClass));
        }

        return UnremovableBeanBuildItem.beanTypes(unremovableClasses);
    }
}
