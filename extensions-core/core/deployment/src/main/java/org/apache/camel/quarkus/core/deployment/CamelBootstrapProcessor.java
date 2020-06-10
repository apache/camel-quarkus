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
package org.apache.camel.quarkus.core.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.runtime.ShutdownContext;
import org.apache.camel.quarkus.core.CamelBootstrapRecorder;
import org.apache.camel.quarkus.core.CamelConfigFlags;
import org.apache.camel.quarkus.core.deployment.spi.CamelBootstrapCompletedBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelRuntimeBuildItem;

class CamelBootstrapProcessor {
    /**
     * Starts the given {@link CamelRuntimeBuildItem}.
     *
     * @param recorder the recorder.
     * @param runtime  a reference to the {@link CamelRuntimeBuildItem}.
     * @param shutdown a reference to a {@link ShutdownContext} used tor register the Camel's related shutdown tasks.
     */
    @BuildStep(onlyIf = { CamelConfigFlags.BootstrapEnabled.class })
    @Record(value = ExecutionTime.RUNTIME_INIT)
    @Produce(CamelBootstrapCompletedBuildItem.class)
    void boot(CamelBootstrapRecorder recorder, CamelRuntimeBuildItem runtime, ShutdownContextBuildItem shutdown) {
        recorder.start(shutdown, runtime.get());
    }
}
