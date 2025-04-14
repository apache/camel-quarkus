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
package org.apache.camel.quarkus.component.jpa.deployment;

import java.util.List;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import jakarta.persistence.EntityManagerFactory;
import org.apache.camel.component.jpa.JpaComponent;
import org.apache.camel.quarkus.component.jpa.CamelJpaProducer;
import org.apache.camel.quarkus.component.jpa.CamelJpaRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelBeanQualifierResolverBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelRuntimeBeanBuildItem;

class JpaProcessor {

    private static final String FEATURE = "camel-jpa";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void configureJpaComponentBean(
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<CamelRuntimeBeanBuildItem> camelRuntimeBean,
            CamelJpaRecorder recorder) {
        additionalBeans.produce(new AdditionalBeanBuildItem(CamelJpaProducer.class));

        camelRuntimeBean.produce(
                new CamelRuntimeBeanBuildItem(
                        "jpa",
                        JpaComponent.class.getName(),
                        recorder.createJpaComponent()));
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void registerPersistenceUnitCamelBeanQualifierResolver(
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptors,
            BuildProducer<CamelBeanQualifierResolverBuildItem> camelBeanQualifierResolver,
            CamelJpaRecorder recorder) {
        // If there are multiple persistence unit configs, then users need to explicitly state which one to use
        // via their component / endpoint configuration. Otherwise if there is just 1, and it is not the default,
        // we can create a resolver for PersistenceUnitLiteral and make named PersistenceUnit autowiring work as expected
        if (persistenceUnitDescriptors.size() == 1) {
            PersistenceUnitDescriptorBuildItem persistenceUnitDescriptor = persistenceUnitDescriptors.get(0);
            if (!persistenceUnitDescriptor.getPersistenceUnitName().equals("<default>")) {
                CamelBeanQualifierResolverBuildItem beanQualifierResolver = new CamelBeanQualifierResolverBuildItem(
                        EntityManagerFactory.class,
                        recorder.createPersistenceUnitQualifierResolver(persistenceUnitDescriptor.getPersistenceUnitName()));
                camelBeanQualifierResolver.produce(beanQualifierResolver);
            }
        }
    }
}
