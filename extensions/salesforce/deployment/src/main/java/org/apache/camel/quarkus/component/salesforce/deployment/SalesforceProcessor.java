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
package org.apache.camel.quarkus.component.salesforce.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.component.salesforce.api.dto.AbstractDTOBase;
import org.apache.camel.quarkus.core.deployment.spi.CamelPackageScanClassBuildItem;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class SalesforceProcessor {

    private static final String SALESFORCE_DTO_PACKAGE = "org.apache.camel.component.salesforce.api.dto";
    private static final String SALESFORCE_INTERNAL_DTO_PACKAGE = "org.apache.camel.component.salesforce.internal.dto";
    private static final String FEATURE = "camel-salesforce";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    void registerForReflection(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<CamelPackageScanClassBuildItem> packageScanClass) {

        IndexView index = combinedIndex.getIndex();

        // NOTE: DTO classes are registered for reflection with fields and methods due to:
        // https://issues.apache.org/jira/browse/CAMEL-16860

        // Register Camel Salesforce DTO classes for reflection
        String[] camelSalesforceDtoClasses = index.getKnownClasses()
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .filter(className -> className.startsWith(SALESFORCE_DTO_PACKAGE)
                        || className.startsWith(SALESFORCE_INTERNAL_DTO_PACKAGE))
                .toArray(String[]::new);

        reflectiveClass.produce(ReflectiveClassBuildItem.builder(camelSalesforceDtoClasses).methods(true).fields(true).build());

        // Register user generated DTOs for reflection
        DotName dtoBaseName = DotName.createSimple(AbstractDTOBase.class.getName());
        String[] userDtoClasses = index.getAllKnownSubclasses(dtoBaseName)
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .filter(className -> !className.startsWith("org.apache.camel.component.salesforce"))
                .toArray(String[]::new);

        reflectiveClass.produce(ReflectiveClassBuildItem.builder(userDtoClasses).methods(true).fields(true).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(KeyStoreParameters.class).methods(true).fields(false).build());

        // Ensure package scanning for user DTO classes can work in native mode
        packageScanClass.produce(new CamelPackageScanClassBuildItem(userDtoClasses));
    }
}
