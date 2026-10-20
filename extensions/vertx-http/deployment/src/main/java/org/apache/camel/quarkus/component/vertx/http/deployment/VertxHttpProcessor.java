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
package org.apache.camel.quarkus.component.vertx.http.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;
import org.apache.camel.component.vertx.http.VertxHttpComponent;
import org.apache.camel.quarkus.component.vertx.http.CamelVertxHttpRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelRuntimeBeanBuildItem;

class VertxHttpProcessor {

    private static final String FEATURE = "camel-vertx-http";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    // TODO: Remove when Quarkus registers io.vertx.core.http.impl.ClientMultipartFormUpload for runtime initialization
    // https://github.com/apache/camel-quarkus/issues/9361
    // Quarkus VertxProcessor still references the Vert.x 4 name io.vertx.ext.web.client.impl.MultipartFormUpload,
    // which no longer exists on Vert.x 5, so the registration is skipped
    @BuildStep
    RuntimeInitializedClassBuildItem runtimeInitializedClasses() {
        return new RuntimeInitializedClassBuildItem("io.vertx.core.http.impl.ClientMultipartFormUpload");
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    CamelRuntimeBeanBuildItem configureVertxComponent(CamelVertxHttpRecorder recorder, VertxBuildItem vertx) {
        return new CamelRuntimeBeanBuildItem("vertx-http", VertxHttpComponent.class.getName(),
                recorder.createVertxHttpComponent(vertx.getVertx()));
    }

}
