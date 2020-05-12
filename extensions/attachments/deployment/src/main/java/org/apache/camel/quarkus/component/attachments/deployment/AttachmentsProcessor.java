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
package org.apache.camel.quarkus.component.attachments.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.apache.camel.quarkus.component.attachments.AttachmentsRecorder;
import org.apache.camel.quarkus.core.UploadAttacher;
import org.apache.camel.quarkus.core.deployment.spi.UploadAttacherBuildItem;

class AttachmentsProcessor {

    private static final String FEATURE = "camel-attachments";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /**
     * Produces the "real" {@link UploadAttacher} thus overriding the default no-op one produced by
     * {@code camel-quarkus-core-deployment}.
     *
     * @param  recorder the {@link AttachmentsRecorder}
     * @return          a new {@link UploadAttacherBuildItem}
     */
    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    UploadAttacherBuildItem uploadAttacher(AttachmentsRecorder recorder) {
        return new UploadAttacherBuildItem(recorder.creatUploadAttacher());
    }

}
