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
package org.apache.camel.quarkus.component.debezium.oracle.deployment;

import io.debezium.connector.oracle.OracleConnector;
import io.debezium.connector.oracle.OracleConnectorTask;
import io.debezium.connector.oracle.OracleSourceInfoStructMaker;
import io.debezium.connector.oracle.logminer.buffered.BufferedLogMinerAdapter;
import io.debezium.connector.oracle.snapshot.query.SelectAllSnapshotQuery;
import io.debezium.storage.kafka.history.KafkaSchemaHistory;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import oracle.jdbc.driver.OracleDriver;

class DebeziumOracleProcessor {

    private static final String FEATURE = "camel-debezium-oracle";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void reflectiveClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {

        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                OracleDriver.class,
                OracleConnector.class,
                OracleConnectorTask.class,
                OracleSourceInfoStructMaker.class,
                SelectAllSnapshotQuery.class,
                BufferedLogMinerAdapter.class,
                KafkaSchemaHistory.class).build());
    }

    @BuildStep
    void addDependencies(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("io.debezium", "debezium-connector-oracle"));
    }

    @BuildStep
    RuntimeInitializedClassBuildItem runtimeInitializedClasses() {
        return new RuntimeInitializedClassBuildItem("com.google.protobuf.JavaFeaturesProto");
    }
}
