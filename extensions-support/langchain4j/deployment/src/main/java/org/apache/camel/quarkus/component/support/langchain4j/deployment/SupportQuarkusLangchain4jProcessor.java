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
package org.apache.camel.quarkus.component.support.langchain4j.deployment;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import dev.langchain4j.guardrail.Guardrail;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.camel.quarkus.component.support.langchain4j.AiToolSpecConverter;
import org.apache.camel.quarkus.component.support.langchain4j.CamelAiToolProvider;
import org.apache.camel.quarkus.component.support.langchain4j.CamelAiToolsInterceptor;
import org.apache.camel.quarkus.component.support.langchain4j.QuarkusLangchain4jRecorder;
import org.apache.camel.quarkus.component.support.langchain4j.RagBridgeConfig;
import org.apache.camel.quarkus.component.support.langchain4j.RagBridgeConfig.AugmentorConfig;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import static io.quarkus.arc.deployment.UnremovableBeanBuildItem.beanClassNames;

/**
 * Build steps required only when Quarkus LangChain4j is detected.
 */
@BuildSteps(onlyIf = QuarkusLangchain4jPresent.class)
class SupportQuarkusLangchain4jProcessor {

    public static final DotName REGISTER_AI_SERVICES_DOTNAME = DotName
            .createSimple("io.quarkiverse.langchain4j.RegisterAiService");
    private static final DotName CAMEL_AI_TOOLS_DOTNAME = DotName
            .createSimple("org.apache.camel.quarkus.component.support.langchain4j.CamelAiTools");

    private static final Logger LOG = Logger.getLogger(SupportQuarkusLangchain4jProcessor.class);

    @BuildStep
    SystemPropertyBuildItem enforceJaxRsHttpClient() {
        LOG.infof("Quarkus LangChain4j detected - enforcing JAX-RS HTTP client factory");
        return new SystemPropertyBuildItem("langchain4j.http.clientBuilderFactory",
                "io.quarkiverse.langchain4j.jaxrsclient.JaxRsHttpClientBuilderFactory");
    }

    @BuildStep
    @SuppressWarnings("unchecked")
    @Record(ExecutionTime.STATIC_INIT)
    void registerLangChain4jAiServiceTypesForReflection(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            QuarkusLangchain4jRecorder recorder) {
        IndexView index = combinedIndex.getIndex();
        // Guardrails are instantiated dynamically
        Set<DotName> guardrailTypes = index.getAllKnownImplementations(InputGuardrail.class)
                .stream()
                .map(ClassInfo::name)
                .collect(Collectors.toSet());

        index.getAllKnownImplementations(OutputGuardrail.class)
                .stream()
                .map(ClassInfo::name)
                .forEach(guardrailTypes::add);

        index.getAllKnownSubclasses(SupportLangchain4jProcessor.JSON_EXTRACTOR_OUTPUT_GUARDRAIL)
                .stream()
                .map(ClassInfo::name)
                .forEach(guardrailTypes::add);

        guardrailTypes.stream()
                .filter(s -> !s.equals(SupportLangchain4jProcessor.JSON_EXTRACTOR_OUTPUT_GUARDRAIL))
                .forEach(s -> {
                    try {
                        Class<Guardrail<?, ?>> guardrailClass;
                        guardrailClass = (Class<Guardrail<?, ?>>) Thread.currentThread()
                                .getContextClassLoader()
                                .loadClass(s.toString());
                        syntheticBeans
                                .produce(SyntheticBeanBuildItem.configure(s)
                                        .scope(Singleton.class)
                                        .named("GuardrailSynthetic" + s.local())
                                        .runtimeValue(recorder.instantiateGuardrails(guardrailClass))
                                        .done());
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void registerQuarkusLangchain4jNativeSupport(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {

        IndexView index = combinedIndex.getIndex();

        // Discover all inner classes of QuarkusJsonCodecFactory
        List<String> codecFactoryClasses = index.getKnownClasses()
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .filter(n -> n.startsWith("io.quarkiverse.langchain4j.QuarkusJsonCodecFactory"))
                .toList();

        LOG.infof("Registered %d QuarkusJsonCodecFactory-related classes for native reflection",
                codecFactoryClasses.size());

        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                codecFactoryClasses.toArray(new String[0]))
                .methods()
                .fields()
                .constructors()
                .build());
    }

    @BuildStep(onlyIf = AiToolPresent.class)
    AdditionalBeanBuildItem registerCamelAiToolProvider() {
        LOG.info("Camel AI Tool detected - registering CamelAiToolProvider as CDI bean for ToolProvider auto-discovery");
        return AdditionalBeanBuildItem.unremovableOf(CamelAiToolProvider.class);
    }

    @BuildStep(onlyIf = AiToolPresent.class)
    void generateAiToolSpecConverter(BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {

        String generatedClassName = "org.apache.camel.quarkus.component.support.langchain4j.AiToolSpecConverterImpl";

        try (ClassCreator cc = ClassCreator.builder()
                .classOutput(new GeneratedBeanGizmoAdaptor(generatedBeans))
                .className(generatedClassName)
                .interfaces(AiToolSpecConverter.class)
                .build()) {

            cc.addAnnotation(Singleton.class);

            String toolSpecClass = "dev.langchain4j.agent.tool.ToolSpecification";
            String aiToolSpecClass = "org.apache.camel.component.ai.tool.AiToolSpec";

            try (MethodCreator mc = cc.getMethodCreator("toToolSpecification",
                    toolSpecClass, aiToolSpecClass)) {

                ResultHandle spec = mc.getMethodParam(0);
                ResultHandle result = mc.invokeStaticMethod(
                        MethodDescriptor.ofMethod(
                                "org.apache.camel.component.langchain4j.agent.AiToolSpecToLangChain4j",
                                "toToolSpecification",
                                toolSpecClass,
                                aiToolSpecClass),
                        spec);
                mc.returnValue(result);
            }
        }

        unremovableBeans.produce(beanClassNames(generatedClassName));
    }

    @BuildStep(onlyIf = AiToolPresent.class)
    @Record(ExecutionTime.STATIC_INIT)
    void configureCamelAiToolTags(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            QuarkusLangchain4jRecorder recorder) {

        IndexView index = combinedIndex.getIndex();
        Map<String, String> tagMap = new HashMap<>();
        for (AnnotationInstance annotation : index.getAnnotations(CAMEL_AI_TOOLS_DOTNAME)) {
            if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                String className = annotation.target().asClass().name().toString();
                if (annotation.value() == null) {
                    LOG.warnf("@CamelAiTools on %s has no value — skipping", className);
                    continue;
                }
                String tagValue = annotation.value().asString();
                if (tagValue.isBlank()) {
                    LOG.warnf("@CamelAiTools on %s has blank value — skipping", className);
                    continue;
                }
                tagMap.put(className, tagValue);
                LOG.infof("Discovered @CamelAiTools(\"%s\") on %s", tagValue, className);
            }
        }

        if (tagMap.isEmpty()) {
            return;
        }

        recorder.setCamelAiToolTagMap(tagMap);

        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClasses(CamelAiToolsInterceptor.class)
                .setUnremovable()
                .build());
    }

    @BuildStep
    void validateAndRegisterAiServices(
            CombinedIndexBuildItem indexBuildItem,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {

        if (!new AiToolPresent().getAsBoolean()) {
            Collection<AnnotationInstance> aiToolsAnnotations = indexBuildItem.getIndex()
                    .getAnnotations(CAMEL_AI_TOOLS_DOTNAME);
            if (!aiToolsAnnotations.isEmpty()) {
                LOG.warnf("@CamelAiTools annotations found but camel-langchain4j-agent is not on the classpath. "
                        + "Add camel-langchain4j-agent dependency to enable the Camel AI tool bridge. "
                        + "Affected classes: %s",
                        aiToolsAnnotations.stream()
                                .filter(a -> a.target().kind() == AnnotationTarget.Kind.CLASS)
                                .map(a -> a.target().asClass().name().toString())
                                .collect(Collectors.joining(", ")));
            }
        }

        LOG.debug("Discovering classes annotated with @RegisterAiService to mark implementation beans as unremovable");

        for (AnnotationInstance instance : indexBuildItem.getIndex().getAnnotations(REGISTER_AI_SERVICES_DOTNAME)) {
            if (instance.target().kind() == AnnotationTarget.Kind.CLASS) {
                String declarativeAiServiceClassName = instance.target().asClass().name().toString();
                LOG.debugf("Marking Quarkus Ai service implementation class for %s as unremovable",
                        declarativeAiServiceClassName);
                unremovableBeans.produce(beanClassNames(declarativeAiServiceClassName + "$$QuarkusImpl"));
            }
        }
    }

    /**
     * Produces {@link RetrievalAugmentor} CDI beans that bridge Camel ingestion routes with
     * {@code @RegisterAiService} RAG.
     *
     * <p>
     * Two modes:
     * <ul>
     * <li><b>Explicit config</b> — each entry under {@code quarkus.camel.langchain4j.rag.augmentors.<name>}
     * produces a {@code @Named("<name>")} RetrievalAugmentor backed by the configured store.
     * If only one entry exists, it is also marked as {@code defaultBean()} for auto-discovery.</li>
     * <li><b>Auto-detection</b> — when no config entries exist, at least one EmbeddingStore and one
     * EmbeddingModel are present, and no RetrievalAugmentor exists yet, a default one is produced
     * backed by the {@code @Default} CDI bean.</li>
     * </ul>
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerDefaultRetrievalAugmentor(
            BeanDiscoveryFinishedBuildItem beanDiscovery,
            RagBridgeConfig ragBridgeConfig,
            QuarkusLangchain4jRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {

        DotName embeddingStoreDN = DotName.createSimple(EmbeddingStore.class.getName());
        DotName embeddingModelDN = DotName.createSimple(EmbeddingModel.class.getName());
        DotName retrievalAugmentorDN = DotName.createSimple(RetrievalAugmentor.class.getName());

        int embeddingStoreCount = 0;
        int embeddingModelCount = 0;
        boolean hasRetrievalAugmentor = false;

        for (BeanInfo bean : beanDiscovery.beanStream().collect(Collectors.toList())) {
            for (org.jboss.jandex.Type type : bean.getTypes()) {
                DotName typeName = type.name();
                if (typeName.equals(embeddingStoreDN)) {
                    embeddingStoreCount++;
                } else if (typeName.equals(embeddingModelDN)) {
                    embeddingModelCount++;
                } else if (typeName.equals(retrievalAugmentorDN)) {
                    hasRetrievalAugmentor = true;
                }
            }
        }

        Map<String, AugmentorConfig> augmentors = ragBridgeConfig.augmentors();

        if (!augmentors.isEmpty()) {
            for (Map.Entry<String, AugmentorConfig> entry : augmentors.entrySet()) {
                String name = entry.getKey();
                AugmentorConfig cfg = entry.getValue();

                LOG.infof("Registering named RetrievalAugmentor '%s' backed by EmbeddingStore '%s'",
                        name, cfg.embeddingStoreName());

                SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                        .configure(RetrievalAugmentor.class)
                        .scope(ApplicationScoped.class)
                        .addQualifier().annotation(Named.class).addValue("value", name).done()
                        .setRuntimeInit()
                        .supplier(recorder.createDefaultRetrievalAugmentorSupplier(
                                cfg.embeddingStoreName(), cfg.embeddingModelName().orElse(null)));

                // Single augmentor configured and no user/Easy-RAG augmentor present:
                // mark as defaultBean() so @RegisterAiService discovers it without a qualifier
                if (augmentors.size() == 1 && !hasRetrievalAugmentor) {
                    configurator.defaultBean();
                }

                syntheticBeans.produce(configurator.done());
            }
            return;
        }

        if (hasRetrievalAugmentor) {
            return;
        }

        if (embeddingStoreCount >= 1 && embeddingModelCount >= 1) {
            LOG.info("EmbeddingStore and EmbeddingModel CDI beans detected"
                    + " - registering default RetrievalAugmentor backed by @Default store");
            syntheticBeans.produce(SyntheticBeanBuildItem
                    .configure(RetrievalAugmentor.class)
                    .scope(ApplicationScoped.class)
                    .defaultBean()
                    .setRuntimeInit()
                    .supplier(recorder.createDefaultRetrievalAugmentorSupplier(null, null))
                    .done());
        }
    }
}
