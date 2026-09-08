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
package org.apache.camel.quarkus.component.langchain4j.agent.deployment;

import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import org.apache.camel.quarkus.component.langchain4j.agent.Langchain4jAgentRecorder;
import org.apache.camel.quarkus.component.support.langchain4j.deployment.QuarkusLangchain4jPresent;
import org.apache.camel.quarkus.core.deployment.spi.RuntimeCamelContextCustomizerBuildItem;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

class Langchain4jAgentProcessor {
    private static final String FEATURE = "camel-langchain4j-agent";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    ReflectiveClassBuildItem registerForReflection(CombinedIndexBuildItem combinedIndex) {
        Set<String> mcpProtocolClasses = combinedIndex.getIndex()
                .getClassesInPackage("dev.langchain4j.mcp.protocol")
                .stream()
                .map(ClassInfo::asClass)
                .map(ClassInfo::name)
                .map(DotName::toString)
                .collect(Collectors.toSet());

        return ReflectiveClassBuildItem
                .builder(mcpProtocolClasses.toArray(new String[0]))
                .fields(true)
                .methods(true)
                .build();
    }

    // The bridge that exposes registry tools to Quarkus LangChain4j AI services ships with camel-quarkus-ai-tool.
    // Without that extension the tools are registered but silently unreachable, so flag it at startup.
    @BuildStep(onlyIf = { QuarkusLangchain4jPresent.class, CamelAiToolBridgeAbsent.class })
    @Record(ExecutionTime.RUNTIME_INIT)
    RuntimeCamelContextCustomizerBuildItem warnAboutMissingAiToolBridge(Langchain4jAgentRecorder recorder) {
        return new RuntimeCamelContextCustomizerBuildItem(recorder.createMissingAiToolBridgeWarning());
    }
}
