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
package org.apache.camel.quarkus.component.azure.deployment;

import com.microsoft.azure.storage.StorageCredentialsAccountAndKey;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.component.azure.blob.BlobServiceConfiguration;
import org.apache.camel.component.azure.common.AbstractConfiguration;
import org.apache.camel.component.azure.queue.QueueServiceConfiguration;
import org.apache.camel.quarkus.core.deployment.UnbannedReflectiveBuildItem;

class AzureProcessor {

    private static final String FEATURE = "camel-azure";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    UnbannedReflectiveBuildItem whitelistConfigurationClasses() {
        return new UnbannedReflectiveBuildItem(BlobServiceConfiguration.class.getName(),
                QueueServiceConfiguration.class.getName());
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection() {
        return new ReflectiveClassBuildItem(true, true, AbstractConfiguration.class, BlobServiceConfiguration.class,
                QueueServiceConfiguration.class,
                StorageCredentialsAccountAndKey.class);
    }

}
