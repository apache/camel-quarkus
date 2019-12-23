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
package org.apache.camel.quarkus.component.aws.s3.deployment;

import java.util.Collection;
import java.util.stream.Collectors;

import com.amazonaws.partitions.model.CredentialScope;
import com.amazonaws.partitions.model.Endpoint;
import com.amazonaws.partitions.model.Partition;
import com.amazonaws.partitions.model.Partitions;
import com.amazonaws.partitions.model.Region;
import com.amazonaws.partitions.model.Service;
import com.amazonaws.services.s3.internal.AWSS3V4Signer;
import com.amazonaws.services.s3.model.CryptoConfiguration;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import org.apache.camel.component.aws.s3.S3Configuration;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class AwsS3Processor {

    public static final String AWS_S3_APPLICATION_ARCHIVE_MARKERS = "com/amazonaws";

    private static final String FEATURE = "camel-aws-s3";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    RuntimeInitializedClassBuildItem cryptoConfiguration() {
        return new RuntimeInitializedClassBuildItem(CryptoConfiguration.class.getCanonicalName());
    }

    @BuildStep(applicationArchiveMarkers = { AWS_S3_APPLICATION_ARCHIVE_MARKERS })
    void process(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageResourceBuildItem> resource) {

        IndexView view = combinedIndexBuildItem.getIndex();

        resource.produce(new NativeImageResourceBuildItem("com/amazonaws/partitions/endpoints.json"));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false,
                Partitions.class.getCanonicalName(),
                Partition.class.getCanonicalName(),
                Endpoint.class.getCanonicalName(),
                Region.class.getCanonicalName(),
                Service.class.getCanonicalName(),
                CredentialScope.class.getCanonicalName(),
                AWSS3V4Signer.class.getCanonicalName(),
                S3Configuration.class.getCanonicalName()));
    }

    protected Collection<String> getImplementations(IndexView view, Class<?> type) {
        return view.getAllKnownImplementors(DotName.createSimple(type.getName())).stream()
                .map(ClassInfo::toString)
                .collect(Collectors.toList());
    }

}
