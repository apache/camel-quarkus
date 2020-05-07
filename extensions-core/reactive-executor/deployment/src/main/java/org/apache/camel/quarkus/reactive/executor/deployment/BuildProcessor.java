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
package org.apache.camel.quarkus.reactive.executor.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.vertx.deployment.VertxBuildItem;
import org.apache.camel.quarkus.core.Flags;
import org.apache.camel.quarkus.core.deployment.spi.CamelInitializedReactiveExecutorBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelReactiveExecutorBuildItem;
import org.apache.camel.quarkus.reactive.executor.ReactiveExecutorRecorder;

public class BuildProcessor {
    @Record(value = ExecutionTime.STATIC_INIT, optional = true)
    @BuildStep(onlyIf = Flags.MainEnabled.class)
    CamelReactiveExecutorBuildItem reactiveExecutor(ReactiveExecutorRecorder recorder) {
        return new CamelReactiveExecutorBuildItem(recorder.createReactiveExecutor());
    }

    @Record(value = ExecutionTime.RUNTIME_INIT, optional = true)
    @BuildStep(onlyIf = Flags.MainEnabled.class)
    CamelInitializedReactiveExecutorBuildItem initReactiveExecutor(
            ReactiveExecutorRecorder recorder, CamelReactiveExecutorBuildItem executor, VertxBuildItem vertx) {
        recorder.initReactiveExecutor(executor.getInstance(), vertx.getVertx());
        return new CamelInitializedReactiveExecutorBuildItem(executor.getInstance());
    }
}
