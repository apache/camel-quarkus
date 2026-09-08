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
package org.apache.camel.quarkus.component.langchain4j.embeddingstore.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
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
import io.quarkus.runtime.configuration.ConfigurationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;
import org.apache.camel.quarkus.component.langchain4j.embeddingstore.Langchain4jEmbeddingstoreRecorder;
import org.apache.camel.quarkus.component.langchain4j.embeddingstore.RagAugmentorName;
import org.apache.camel.quarkus.component.langchain4j.embeddingstore.RagBridgeConfig;
import org.apache.camel.quarkus.component.langchain4j.embeddingstore.RagBridgeConfig.AugmentorConfig;
import org.apache.camel.quarkus.component.langchain4j.embeddingstore.RagRetrievalFilterSupplier;
import org.apache.camel.quarkus.core.deployment.spi.CamelRegistryBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelRuntimeTaskBuildItem;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

/**
 * Bridges Camel ingestion routes with Quarkus LangChain4j RAG. Registered only when Quarkus
 * LangChain4j is detected.
 */
@BuildSteps(onlyIf = QuarkusLangchain4jPresent.class)
class Langchain4jEmbeddingstoreRagProcessor {

    private static final DotName EMBEDDING_STORE_NAME_DOTNAME = DotName
            .createSimple("io.quarkiverse.langchain4j.EmbeddingStoreName");
    private static final DotName RAG_RETRIEVAL_FILTER_SUPPLIER_DOTNAME = DotName
            .createSimple(RagRetrievalFilterSupplier.class.getName());
    private static final DotName DEPENDENT_DOTNAME = DotName.createSimple(Dependent.class.getName());

    private static final Logger LOG = Logger.getLogger(Langchain4jEmbeddingstoreRagProcessor.class);

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
            Langchain4jEmbeddingstoreRecorder recorder,
            CamelRegistryBuildItem registry) {
        recorder.registerNamedEmbeddingStores(registry.getRegistry());
        return new CamelRuntimeTaskBuildItem("named-embedding-stores");
    }

    // RagAugmentorName carries @Qualifier, but it lives in the extension runtime artifact, which
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
            Langchain4jEmbeddingstoreRecorder recorder,
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
