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
package org.apache.camel.quarkus.component.langchain4j.ingest.deployment;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.runtime.configuration.ConfigurationException;
import jakarta.inject.Singleton;
import org.apache.camel.quarkus.component.langchain4j.ingest.Ingest;
import org.apache.camel.quarkus.component.langchain4j.ingest.IngestBuildTimeConfig;
import org.apache.camel.quarkus.component.langchain4j.ingest.IngestBuilderPipelines;
import org.apache.camel.quarkus.component.langchain4j.ingest.IngestPipeline;
import org.apache.camel.quarkus.component.langchain4j.ingest.IngestRoutes;
import org.apache.camel.quarkus.component.langchain4j.ingest.Langchain4jIngestRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelContextBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelRuntimeTaskBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceBuildItem;
import org.apache.camel.quarkus.core.deployment.util.CamelSupport;
import org.apache.camel.quarkus.core.deployment.util.PathFilter;
import org.apache.camel.util.URISupport;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

class Langchain4jIngestProcessor {

    private static final String FEATURE = "camel-langchain4j-ingest";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem beans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(IngestRoutes.class)
                .setUnremovable()
                .build();
    }

    /**
     * Discovers {@code @Ingest} builder methods: validated here (return type, no parameters,
     * unique names, no collision with configuration-declared pipelines), invoked reflectively once
     * at startup.
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void discoverBuilderPipelines(
            CombinedIndexBuildItem combinedIndex,
            IngestBuildTimeConfig config,
            Langchain4jIngestRecorder recorder,
            BuildProducer<AdditionalBeanBuildItem> beans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {

        DotName ingestAnnotation = DotName.createSimple(Ingest.class.getName());
        DotName pipelineType = DotName.createSimple(IngestPipeline.class.getName());

        List<String> flatEntries = new ArrayList<>();
        Set<String> names = new HashSet<>();
        Set<String> beanClasses = new HashSet<>();

        for (AnnotationInstance annotation : combinedIndex.getIndex().getAnnotations(ingestAnnotation)) {
            if (annotation.target().kind() != AnnotationTarget.Kind.METHOD) {
                continue;
            }
            MethodInfo method = annotation.target().asMethod();
            String name = annotation.value().asString();
            String location = method.declaringClass().name() + "#" + method.name();

            if (name.isBlank()) {
                throw new ConfigurationException("@Ingest on " + location + " has a blank pipeline name");
            }
            if (!method.returnType().name().equals(pipelineType)) {
                throw new ConfigurationException("@Ingest method " + location + " must return "
                        + IngestPipeline.class.getSimpleName());
            }
            if (!method.parameters().isEmpty()) {
                throw new ConfigurationException("@Ingest method " + location + " must take no parameters");
            }
            // the method is invoked on a CDI bean instance, which a static method would bypass
            // and a private one would run against the client proxy, seeing null injected fields
            if (Modifier.isPrivate(method.flags()) || Modifier.isStatic(method.flags())) {
                throw new ConfigurationException("@Ingest method " + location + " must not be private or static");
            }
            if (!names.add(name) || config.pipelines().containsKey(name)) {
                throw new ConfigurationException("Ingestion pipeline '" + name + "' is declared more than once "
                        + "(builder and/or configuration). Pipeline names must be unique.");
            }

            flatEntries.add(name);
            flatEntries.add(method.declaringClass().name().toString());
            flatEntries.add(method.name());
            beanClasses.add(method.declaringClass().name().toString());
        }

        if (!beanClasses.isEmpty()) {
            beans.produce(AdditionalBeanBuildItem.builder()
                    .addBeanClasses(beanClasses.toArray(new String[0]))
                    .setUnremovable()
                    .build());
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(beanClasses.toArray(new String[0]))
                    .methods()
                    .build());
        }

        syntheticBeans.produce(SyntheticBeanBuildItem.configure(IngestBuilderPipelines.class)
                .scope(Singleton.class)
                .unremovable()
                .runtimeValue(recorder.createBuilderPipelines(flatEntries))
                .done());
    }

    /**
     * The pre-start half of the component-presence check: recorded as a Camel runtime task, it
     * runs after ArC is fully initialised but before the Camel runtime is assembled — and thus
     * before Camel Main binds {@code camel.component.*} properties, whose failure for a missing
     * component would otherwise preempt the friendlier add-extension hint.
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    CamelRuntimeTaskBuildItem checkComponentsPresent(Langchain4jIngestRecorder recorder,
            CamelContextBuildItem camelContext) {
        recorder.checkComponentsPresent(camelContext.getCamelContext());
        return new CamelRuntimeTaskBuildItem("langchain4j-ingest-components");
    }

    /**
     * A pipeline whose consumer URI names a component that is not on the classpath stops the
     * build, with the command that fixes it rather than a startup failure. Only configured URIs
     * can be checked: a builder-declared pipeline composes its URI at startup, where the pre-start
     * task above applies the same hint. Component services are REGISTRY-destination, so they are
     * read from the application archives directly — they never appear among the DISCOVERY
     * {@code CamelServiceBuildItem}s.
     */
    @BuildStep
    void validateConnectorsPresent(IngestBuildTimeConfig config, ApplicationArchivesBuildItem applicationArchives,
            BuildProducer<ValidationErrorBuildItem> validationErrors) {
        PathFilter pathFilter = new PathFilter.Builder()
                .include("META-INF/services/org/apache/camel/component/*")
                .build();
        Set<String> components = CamelSupport.services(applicationArchives, pathFilter)
                .map(CamelServiceBuildItem::getName)
                .collect(Collectors.toSet());

        for (Map.Entry<String, IngestBuildTimeConfig.PipelineBuildTimeConfig> entry : config.pipelines().entrySet()) {
            String uri = entry.getValue().source().uri().orElse(null);
            if (uri == null) {
                continue;
            }
            int colon = uri.indexOf(':');
            String scheme = colon < 1 ? uri : uri.substring(0, colon);
            // a placeholder resolves at startup, so its scheme cannot be known here
            if (scheme.contains("{{") || scheme.contains("$")) {
                continue;
            }
            if (!components.contains(scheme)) {
                // the URI is sanitized: a consumer URI may legitimately carry credentials, and a
                // build log is no place for them. The artifact hint is a heuristic - multi-scheme
                // components (smtp -> camel-quarkus-mail) name their extension differently
                validationErrors.produce(new ValidationErrorBuildItem(new ConfigurationException(
                        "Ingestion pipeline '" + entry.getKey() + "' consumes from '"
                                + URISupport.sanitizeUri(uri) + "', but the Camel component '" + scheme
                                + "' is not on the classpath.\nAdd the extension that provides it, usually:"
                                + "  ./mvnw quarkus:add-extension -Dextensions=camel-quarkus-" + scheme)));
            }
        }
    }

    /**
     * What can be decided from build-time configuration fails here, at build time, with the fix in
     * the message — never silently at runtime.
     */
    @BuildStep
    void validatePipelines(IngestBuildTimeConfig config, BuildProducer<ValidationErrorBuildItem> validationErrors) {
        for (Map.Entry<String, IngestBuildTimeConfig.PipelineBuildTimeConfig> entry : config.pipelines().entrySet()) {
            IngestBuildTimeConfig.PipelineBuildTimeConfig pipeline = entry.getValue();

            if (pipeline.maxSegmentSize() <= 0 || pipeline.maxOverlapSize() < 0
                    || pipeline.maxOverlapSize() >= pipeline.maxSegmentSize()) {
                validationErrors.produce(new ValidationErrorBuildItem(new ConfigurationException(
                        "Ingestion pipeline '" + entry.getKey() + "': max-segment-size must be positive and "
                                + "max-overlap-size must be smaller than it (got " + pipeline.maxSegmentSize()
                                + " / " + pipeline.maxOverlapSize() + ")")));
            }
        }
    }
}
