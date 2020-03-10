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
package org.apache.camel.quarkus.component.support.policy.deployment;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.quarkus.core.deployment.UnbannedReflectiveBuildItem;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.DotName;

class PolicyProcessor {

    private static final String FEATURE = "camel-policy";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    UnbannedReflectiveBuildItem unbanReflectives() {
        /**
         * A list of classes annotated with <code>@UriParams</code> which we accept to be registered for reflection
         * mostly because there are errors when they are removed. TODO: solve the underlying problems and remove as
         * many entries as possible from the list.
         */
        return new UnbannedReflectiveBuildItem(
                "org.apache.camel.support.processor.DefaultExchangeFormatter",
                "org.apache.camel.component.pdf.PdfConfiguration",
                "org.apache.camel.component.netty.NettyConfiguration",
                "org.apache.camel.component.netty.NettyServerBootstrapConfiguration",
                "org.apache.camel.component.fhir.FhirUpdateEndpointConfiguration",
                "org.apache.camel.component.fhir.FhirOperationEndpointConfiguration",
                "org.apache.camel.component.fhir.FhirConfiguration",
                "org.apache.camel.component.fhir.FhirLoadPageEndpointConfiguration",
                "org.apache.camel.component.fhir.FhirSearchEndpointConfiguration",
                "org.apache.camel.component.fhir.FhirTransactionEndpointConfiguration",
                "org.apache.camel.component.fhir.FhirCreateEndpointConfiguration",
                "org.apache.camel.component.fhir.FhirValidateEndpointConfiguration",
                "org.apache.camel.component.fhir.FhirReadEndpointConfiguration",
                "org.apache.camel.component.fhir.FhirCapabilitiesEndpointConfiguration",
                "org.apache.camel.component.fhir.FhirHistoryEndpointConfiguration",
                "org.apache.camel.component.fhir.FhirMetaEndpointConfiguration",
                "org.apache.camel.component.fhir.FhirPatchEndpointConfiguration",
                "org.apache.camel.component.fhir.FhirDeleteEndpointConfiguration");
    }

    /* Make the build fail as long as there are banned classes registered for reflection */
    @BuildStep
    void bannedReflectiveClasses(
            CombinedIndexBuildItem combinedIndex,
            List<ReflectiveClassBuildItem> reflectiveClasses,
            List<UnbannedReflectiveBuildItem> unbannedReflectives,
            BuildProducer<GeneratedResourceBuildItem> dummy // to force the execution of this method
    ) {
        final DotName uriParamsDotName = DotName.createSimple("org.apache.camel.spi.UriParams");

        final Set<String> bannedClassNames = combinedIndex.getIndex()
                .getAnnotations(uriParamsDotName)
                .stream()
                .filter(ai -> ai.target().kind() == Kind.CLASS)
                .map(ai -> ai.target().asClass().name().toString())
                .collect(Collectors.toSet());

        final Set<String> unbannedClassNames = unbannedReflectives.stream()
                .map(UnbannedReflectiveBuildItem::getClassNames)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        Set<String> violations = reflectiveClasses.stream()
                .map(ReflectiveClassBuildItem::getClassNames)
                .flatMap(Collection::stream)
                .filter(cl -> !unbannedClassNames.contains(cl))
                .filter(bannedClassNames::contains)
                .collect(Collectors.toSet());

        if (!violations.isEmpty()) {
            throw new IllegalStateException(
                    "The following classes should either be whitelisted via an UnbannedReflectiveBuildItem or they should not be registered for reflection via ReflectiveClassBuildItem: "
                            + violations);
        }
    }
}
