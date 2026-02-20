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
package org.apache.camel.quarkus.component.keycloak.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.BouncyIntegration;

class KeycloakProcessor {

    private static final String FEATURE = "camel-keycloak";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {
        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem(BouncyIntegration.class.getName()));
        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem(CryptoIntegration.class.getName()));
    }

    @BuildStep
    void registerServiceProviders(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        String[] spis = {
                "org.keycloak.common.crypto.CryptoProvider",
                "org.keycloak.protocol.oidc.client.authentication.ClientCredentialsProvider"
        };

        for (String spi : spis) {
            serviceProvider.produce(ServiceProviderBuildItem.allProvidersFromClassPath(spi));
        }
    }

}
