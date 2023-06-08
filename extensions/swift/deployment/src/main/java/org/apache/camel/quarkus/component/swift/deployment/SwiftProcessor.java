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
package org.apache.camel.quarkus.component.swift.deployment;

import com.prowidesoftware.swift.model.AbstractMessage;
import com.prowidesoftware.swift.model.MessageStandardType;
import com.prowidesoftware.swift.model.SwiftBlock;
import com.prowidesoftware.swift.model.Tag;
import com.prowidesoftware.swift.model.UnparsedTextList;
import com.prowidesoftware.swift.model.field.Field;
import com.prowidesoftware.swift.model.field.Narrative;
import com.prowidesoftware.swift.model.field.StructuredNarrative;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class SwiftProcessor {

    private static final String FEATURE = "camel-swift";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void indexDependencies(BuildProducer<IndexDependencyBuildItem> indexedDependency) {
        // The dependency for MX message types
        // Adding this dependency slows down drastically the native image build since it brings a lot of reflection
        // indexedDependency.produce(new IndexDependencyBuildItem("com.prowidesoftware", "pw-iso20022"));
        // The dependency for MT message types
        indexedDependency.produce(new IndexDependencyBuildItem("com.prowidesoftware", "pw-swift-core"));
    }

    @BuildStep
    void registerOtherTypesForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        reflectiveClass.produce(
                ReflectiveClassBuildItem
                        .builder(Tag.class, UnparsedTextList.class, MessageStandardType.class, Narrative.class,
                                StructuredNarrative.class)
                        .fields()
                        .build());

    }

    @BuildStep
    void registerMessageTypesForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            CombinedIndexBuildItem combinedIndex) {
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(AbstractMessage.class)
                .methods().fields().build());
        registerForReflection(combinedIndex, AbstractMessage.class.getName(), reflectiveClass);
    }

    @BuildStep
    void registerFieldTypesForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            CombinedIndexBuildItem combinedIndex) {

        registerForReflection(combinedIndex, Field.class.getName(), reflectiveClass);
    }

    @BuildStep
    void registerSwiftBlocksForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            CombinedIndexBuildItem combinedIndex) {
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(SwiftBlock.class).fields().build());
        registerForReflection(combinedIndex, SwiftBlock.class.getName(), reflectiveClass);
    }

    private static void registerForReflection(CombinedIndexBuildItem combinedIndex, String className,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        IndexView index = combinedIndex.getIndex();

        String[] classes = index.getAllKnownSubclasses(DotName.createSimple(className))
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .toArray(String[]::new);

        // Register classes for refection
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(classes).fields().build());
    }
}
