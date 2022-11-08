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
package org.apache.camel.quarkus.component.file.cluster.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.runtime.RuntimeValue;
import org.apache.camel.component.file.cluster.FileLockClusterService;
import org.apache.camel.quarkus.component.file.cluster.FileLockClusterServiceConfig;
import org.apache.camel.quarkus.component.file.cluster.FileLockClusterServiceRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelBeanBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelContextBuildItem;

class FileLockClusterServiceProcessor {

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep(onlyIf = FileLockClusterServiceConfig.Enabled.class)
    @Consume(CamelContextBuildItem.class)
    CamelBeanBuildItem setupFileLockClusterService(FileLockClusterServiceConfig config,
            FileLockClusterServiceRecorder recorder) {

        final RuntimeValue<FileLockClusterService> flcs = recorder.createFileLockClusterService(config);
        return new CamelBeanBuildItem("fileLockClusterService", FileLockClusterService.class.getName(), flcs);
    }
}
