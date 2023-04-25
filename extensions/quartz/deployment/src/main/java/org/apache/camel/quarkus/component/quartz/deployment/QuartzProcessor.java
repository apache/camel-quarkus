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
package org.apache.camel.quarkus.component.quartz.deployment;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import org.apache.camel.component.quartz.QuartzComponent;
import org.apache.camel.quarkus.component.quartz.CamelQuartzRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelBeanBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelContextCustomizerBuildItem;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.quartz.impl.jdbcjobstore.StdJDBCDelegate;

class QuartzProcessor {

    private static final String FEATURE = "camel-quartz";
    private static final String[] QUARTZ_JOB_CLASSES = new String[] {
            "org.apache.camel.component.quartz.CamelJob",
            "org.apache.camel.component.quartz.StatefulCamelJob",
            "org.apache.camel.pollconsumer.quartz.QuartzScheduledPollConsumerJob",
            "org.quartz.utils.C3p0PoolingConnectionProvider"
    };
    private static final String[] QUARTZ_JOB_CLASSES_WITH_METHODS = new String[] {
            "org.quartz.impl.jdbcjobstore.JobStoreTX",
            "org.quartz.impl.jdbcjobstore.JobStoreSupport",
            "org.quartz.impl.triggers.SimpleTriggerImpl",
            "org.quartz.impl.triggers.AbstractTrigger",
    };
    private static final DotName SQL_JDBC_DELEGATE = DotName.createSimple(StdJDBCDelegate.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    NativeImageResourceBuildItem nativeImageResources() {
        return new NativeImageResourceBuildItem("org/quartz/quartz.properties");
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection() {
        return ReflectiveClassBuildItem.builder(QUARTZ_JOB_CLASSES).build();
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflectionWithMethods() {
        return ReflectiveClassBuildItem.builder(QUARTZ_JOB_CLASSES_WITH_METHODS).methods().build();
    }

    @BuildStep
    void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            CombinedIndexBuildItem combinedIndex, CurateOutcomeBuildItem curateOutcome) {
        IndexView index = combinedIndex.getIndex();

        ApplicationModel applicationModel = curateOutcome.getApplicationModel();
        boolean oracleBlobIsPresent = applicationModel.getDependencies().stream()
                .anyMatch(d -> d.getGroupId().equals("com.oracle.database.jdbc"));

        final String[] delegatesImpl = index
                .getAllKnownSubclasses(SQL_JDBC_DELEGATE)
                .stream()
                .map(c -> c.name().toString())
                .filter(n -> oracleBlobIsPresent || !n.contains("oracle"))
                .toArray(String[]::new);

        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(delegatesImpl).fields().build());
    }

    @BuildStep
    void indexDependencies(BuildProducer<IndexDependencyBuildItem> indexedDependency) {
        indexedDependency.produce(new IndexDependencyBuildItem("org.quartz-scheduler", "quartz"));
    }

    @BuildStep
    NativeImageSystemPropertyBuildItem disableJMX() {
        return new NativeImageSystemPropertyBuildItem("com.mchange.v2.c3p0.management.ManagementCoordinator",
                "com.mchange.v2.c3p0.management.NullManagementCoordinator");
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelBeanBuildItem quartzComponent(CamelQuartzRecorder recorder) {
        return new CamelBeanBuildItem(
                "quartz",
                QuartzComponent.class.getName(),
                recorder.createQuartzComponent());
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelContextCustomizerBuildItem createQuartzAutowiredLifecycleStrategyContextCustomizer(CamelQuartzRecorder recorder) {
        return new CamelContextCustomizerBuildItem(recorder.createQuartzAutowiredLifecycleStrategy());
    }
}
