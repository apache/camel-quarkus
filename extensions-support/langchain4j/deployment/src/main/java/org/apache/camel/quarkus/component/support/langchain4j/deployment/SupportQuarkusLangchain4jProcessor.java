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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
import io.quarkus.arc.deployment.QualifierRegistrarBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Consume;
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
import io.quarkus.runtime.configuration.ConfigurationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.camel.quarkus.component.support.langchain4j.AiToolSpecConverter;
import org.apache.camel.quarkus.component.support.langchain4j.CamelAiToolProvider;
import org.apache.camel.quarkus.component.support.langchain4j.CamelAiToolsInterceptor;
import org.apache.camel.quarkus.component.support.langchain4j.QuarkusLangchain4jRecorder;
import org.apache.camel.quarkus.component.support.langchain4j.RagAugmentorName;
import org.apache.camel.quarkus.component.support.langchain4j.RagBridgeConfig;
import org.apache.camel.quarkus.component.support.langchain4j.RagBridgeConfig.AugmentorConfig;
import org.apache.camel.quarkus.component.support.langchain4j.RagRetrievalFilterSupplier;
import org.apache.camel.quarkus.core.deployment.spi.CamelRegistryBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelRuntimeTaskBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
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
    private static final DotName EMBEDDING_STORE_NAME_DOTNAME = DotName
            .createSimple("io.quarkiverse.langchain4j.EmbeddingStoreName");
    private static final DotName RAG_RETRIEVAL_FILTER_SUPPLIER_DOTNAME = DotName
            .createSimple(RagRetrievalFilterSupplier.class.getName());
    private static final DotName DEPENDENT_DOTNAME = DotName.createSimple(Dependent.class.getName());

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

    // The retrieval filter supplier is only resolved programmatically at augmentor creation
    // time, so ArC would remove a user's implementation as unused.
    @BuildStep
    UnremovableBeanBuildItem retainRagRetrievalFilterSuppliers() {
        return UnremovableBeanBuildItem.beanTypes(RAG_RETRIEVAL_FILTER_SUPPLIER_DOTNAME);
    }

    /**
     * The retrieval filter is an access control, so every way of disabling it silently is a build
     * failure or a warning: two implementations make the lookup ambiguous, a {@code @Dependent}
     * one is resolved once and never destroyed, and a filter no produced augmentor will ever
     * consult isolates nothing.
     */
    @BuildStep
    void validateRagRetrievalFilterSupplier(BeanDiscoveryFinishedBuildItem beanDiscovery,
            RagBridgeConfig ragBridgeConfig,
            BuildProducer<ValidationErrorBuildItem> validationErrors) {
        BeanCensus census = BeanCensus.of(beanDiscovery);
        if (census.filterSuppliers().isEmpty()) {
            return;
        }

        if (census.filterSuppliers().size() > 1) {
            validationErrors.produce(new ValidationErrorBuildItem(new ConfigurationException(
                    "Found " + census.filterSuppliers().size() + " RagRetrievalFilterSupplier beans ("
                            + census.filterSuppliers().stream().map(bean -> bean.getBeanClass().toString())
                                    .collect(Collectors.joining(", "))
                            + "). Exactly one is allowed: an ambiguous lookup would leave every retrieval "
                            + "unfiltered instead of failing.")));
            return;
        }

        BeanInfo supplier = census.filterSuppliers().get(0);
        if (DEPENDENT_DOTNAME.equals(supplier.getScope().getDotName())) {
            validationErrors.produce(new ValidationErrorBuildItem(new ConfigurationException(
                    "RagRetrievalFilterSupplier bean " + supplier.getBeanClass()
                            + " is @Dependent. Use @ApplicationScoped, @RequestScoped or @Singleton: the supplier "
                            + "is resolved once per augmentor, so a @Dependent instance would never be destroyed.")));
            return;
        }

        if (!census.producesAugmentor(ragBridgeConfig, new EasyRagPresent().getAsBoolean())) {
            LOG.warnf("RagRetrievalFilterSupplier bean %s will never be consulted: it filters only the "
                    + "RetrievalAugmentors produced by this extension, and none is produced here (Easy RAG or an "
                    + "application-provided RetrievalAugmentor takes over). Retrieval is NOT filtered.",
                    supplier.getBeanClass());
        }
    }

    // A store declared only for use from a Camel route is never injected anywhere in Java,
    // so ArC would remove it as unused and registerNamedEmbeddingStores would find nothing.
    @BuildStep
    UnremovableBeanBuildItem retainNamedEmbeddingStores() {
        return new UnremovableBeanBuildItem(bean -> bean.getQualifiers()
                .stream()
                .anyMatch(qualifier -> qualifier.name().equals(EMBEDDING_STORE_NAME_DOTNAME)));
    }

    // Bridges @EmbeddingStoreName-qualified CDI beans into the Camel registry so routes
    // can reference them by name (e.g. embeddingStore=#products) without manual binding.
    // Stores declared via Quarkus LangChain4j configuration (pgvector, redis, ...) are
    // runtime-init synthetic beans, so the scan must not run before ArC initializes them.
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    CamelRuntimeTaskBuildItem registerNamedEmbeddingStores(
            QuarkusLangchain4jRecorder recorder,
            CamelRegistryBuildItem registry) {
        recorder.registerNamedEmbeddingStores(registry.getRegistry());
        return new CamelRuntimeTaskBuildItem("named-embedding-stores");
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

    // RagAugmentorName carries @Qualifier, but it lives in the support runtime artifact, which
    // ships no Jandex index. Without this registration ArC does not know the annotation at all
    // and the synthetic bean below fails the build with
    // "Annotation class not available: @RagAugmentorName".
    @BuildStep
    QualifierRegistrarBuildItem registerRagAugmentorNameQualifier() {
        return new QualifierRegistrarBuildItem(
                () -> Map.of(DotName.createSimple(RagAugmentorName.class.getName()), Set.of()));
    }

    /**
     * Produces {@link RetrievalAugmentor} CDI beans that bridge Camel ingestion routes with
     * {@code @RegisterAiService} RAG.
     *
     * <p>
     * Two modes:
     * <ul>
     * <li><b>Explicit config</b> — each entry under {@code quarkus.camel.langchain4j.rag.augmentors.<name>}
     * produces a {@code @Named("<name>")} RetrievalAugmentor backed by the configured store. The entry
     * marked {@code default=true} also serves the unqualified lookup; with a single entry that marking
     * is optional, with several it is required.</li>
     * <li><b>Auto-detection</b> — when no config entries exist, at least one EmbeddingStore and one
     * EmbeddingModel are present, and no RetrievalAugmentor exists yet, a default one is produced
     * backed by the {@code @Default} CDI bean.</li>
     * </ul>
     */
    @BuildStep(onlyIfNot = EasyRagPresent.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerDefaultRetrievalAugmentor(
            BeanDiscoveryFinishedBuildItem beanDiscovery,
            RagBridgeConfig ragBridgeConfig,
            QuarkusLangchain4jRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {

        BeanCensus census = BeanCensus.of(beanDiscovery);

        // Effective augmentors: one per explicit config entry. Sorted, so that a message naming
        // them reads the same on every build - SmallRye's map is not declaration-ordered.
        Map<String, AugmentorDefinition> effective = new TreeMap<>();
        for (Map.Entry<String, AugmentorConfig> entry : ragBridgeConfig.augmentors().entrySet()) {
            AugmentorConfig cfg = entry.getValue();
            effective.put(entry.getKey(), new AugmentorDefinition(
                    cfg.embeddingStoreName(), cfg.embeddingModelName().orElse(null), cfg.defaultAugmentor()));
        }

        if (!effective.isEmpty()) {
            String designatedDefault = resolveDesignatedDefault(effective, census.retrievalAugmentor());

            for (Map.Entry<String, AugmentorDefinition> entry : effective.entrySet()) {
                String name = entry.getKey();
                AugmentorDefinition def = entry.getValue();

                LOG.debugf("Registering named RetrievalAugmentor '%s' backed by EmbeddingStore '%s'%s",
                        name, def.embeddingStoreName(), name.equals(designatedDefault) ? " (default)" : "");

                SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                        .configure(RetrievalAugmentor.class)
                        .scope(ApplicationScoped.class)
                        .addQualifier().annotation(Named.class).addValue("value", name).done()
                        .setRuntimeInit()
                        .supplier(recorder.createRetrievalAugmentorSupplier(
                                def.embeddingStoreName(), def.embeddingModelName(), name));

                if (name.equals(designatedDefault)) {
                    // keeps @Named only: the implicit @Default makes it the one candidate the
                    // unqualified Instance<RetrievalAugmentor> lookup of Quarkus LangChain4j sees
                    configurator.defaultBean();
                } else {
                    // a real qualifier suppresses the implicit @Default (CDI rule), so this bean
                    // stays selectable by name without making the unqualified lookup ambiguous
                    configurator.addQualifier().annotation(RagAugmentorName.class).addValue("value", name).done();
                }

                syntheticBeans.produce(configurator.done());
            }
            return;
        }

        if (census.retrievalAugmentor()) {
            return;
        }

        if (census.embeddingStores() >= 1 && census.embeddingModels() >= 1) {
            LOG.debug("EmbeddingStore and EmbeddingModel CDI beans detected"
                    + " - registering default RetrievalAugmentor backed by @Default store");
            syntheticBeans.produce(SyntheticBeanBuildItem
                    .configure(RetrievalAugmentor.class)
                    .scope(ApplicationScoped.class)
                    .defaultBean()
                    .setRuntimeInit()
                    .supplier(recorder.createRetrievalAugmentorSupplier(null, null, null))
                    .done());
        }
    }

    /**
     * Decides which augmentor is the unqualified default, or fails the build: silence here would
     * mean an ambiguous CDI lookup and RAG silently switched off for every AI service.
     */
    static String resolveDesignatedDefault(Map<String, AugmentorDefinition> effective,
            boolean hasRetrievalAugmentor) {
        List<String> marked = effective.entrySet().stream()
                .filter(e -> e.getValue().markedDefault())
                .map(Map.Entry::getKey)
                .toList();

        if (marked.size() > 1) {
            throw new ConfigurationException(
                    "Multiple retrieval augmentors are marked default: " + marked + ". Mark exactly one with "
                            + "quarkus.camel.langchain4j.rag.augmentors.<name>.default=true");
        }

        if (hasRetrievalAugmentor) {
            // a user-provided RetrievalAugmentor bean already serves the unqualified lookup
            if (marked.size() == 1) {
                throw new ConfigurationException(
                        "Retrieval augmentor '" + marked.get(0) + "' is marked default, but the application "
                                + "already provides a RetrievalAugmentor bean. Remove the default marking — "
                                + "produced augmentors remain selectable by name.");
            }
            return null;
        }

        if (marked.size() == 1) {
            return marked.get(0);
        }

        if (effective.size() == 1) {
            return effective.keySet().iterator().next();
        }

        throw new ConfigurationException(
                effective.size() + " retrieval augmentors are configured (" + String.join(", ", effective.keySet())
                        + ") but none is marked default. An unmarked ambiguity would silently disable RAG for "
                        + "every AI service, so the build stops instead. Mark exactly one with "
                        + "quarkus.camel.langchain4j.rag.augmentors.<name>.default=true");
    }

    record AugmentorDefinition(String embeddingStoreName, String embeddingModelName, boolean markedDefault) {
    }

    /** One pass over the discovered beans, answering everything the RAG bridge decides on. */
    record BeanCensus(int embeddingStores, int embeddingModels, boolean retrievalAugmentor,
            List<BeanInfo> filterSuppliers) {

        static BeanCensus of(BeanDiscoveryFinishedBuildItem beanDiscovery) {
            DotName embeddingStoreDN = DotName.createSimple(EmbeddingStore.class.getName());
            DotName embeddingModelDN = DotName.createSimple(EmbeddingModel.class.getName());
            DotName retrievalAugmentorDN = DotName.createSimple(RetrievalAugmentor.class.getName());

            int embeddingStores = 0;
            int embeddingModels = 0;
            boolean retrievalAugmentor = false;
            List<BeanInfo> filterSuppliers = new ArrayList<>();

            for (BeanInfo bean : beanDiscovery.beanStream().collect(Collectors.toList())) {
                for (Type type : bean.getTypes()) {
                    DotName typeName = type.name();
                    if (typeName.equals(embeddingStoreDN)) {
                        embeddingStores++;
                    } else if (typeName.equals(embeddingModelDN)) {
                        embeddingModels++;
                    } else if (typeName.equals(retrievalAugmentorDN)) {
                        retrievalAugmentor = true;
                    } else if (typeName.equals(RAG_RETRIEVAL_FILTER_SUPPLIER_DOTNAME)) {
                        filterSuppliers.add(bean);
                    }
                }
            }
            return new BeanCensus(embeddingStores, embeddingModels, retrievalAugmentor, filterSuppliers);
        }

        /** Whether {@link #registerDefaultRetrievalAugmentor} will produce an augmentor to filter. */
        boolean producesAugmentor(RagBridgeConfig ragBridgeConfig, boolean easyRagPresent) {
            if (easyRagPresent) {
                return false;
            }
            if (!ragBridgeConfig.augmentors().isEmpty()) {
                return true;
            }
            return !retrievalAugmentor && embeddingStores >= 1 && embeddingModels >= 1;
        }
    }
}
