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
package org.apache.camel.quarkus.component.jta.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.quarkus.component.jta.MandatoryJtaTransactionPolicy;
import org.apache.camel.quarkus.component.jta.NeverJtaTransactionPolicy;
import org.apache.camel.quarkus.component.jta.NotSupportedJtaTransactionPolicy;
import org.apache.camel.quarkus.component.jta.RequiredJtaTransactionPolicy;
import org.apache.camel.quarkus.component.jta.RequiresNewJtaTransactionPolicy;
import org.apache.camel.quarkus.component.jta.SupportsJtaTransactionPolicy;

class JtaProcessor {

    private static final String FEATURE = "camel-jta";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void transactedPolicy(
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            Capabilities capabilities) {
        if (capabilities.isPresent(Capability.TRANSACTIONS)) {
            AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
            builder.addBeanClass(RequiredJtaTransactionPolicy.class);
            builder.addBeanClass(RequiresNewJtaTransactionPolicy.class);
            builder.addBeanClass(MandatoryJtaTransactionPolicy.class);
            builder.addBeanClass(NeverJtaTransactionPolicy.class);
            builder.addBeanClass(NotSupportedJtaTransactionPolicy.class);
            builder.addBeanClass(SupportsJtaTransactionPolicy.class);

            additionalBeans.produce(builder.build());

            reflectiveClass.produce(ReflectiveClassBuildItem.builder(IllegalStateException.class.getName())
                    .build());
        }
    }
}
