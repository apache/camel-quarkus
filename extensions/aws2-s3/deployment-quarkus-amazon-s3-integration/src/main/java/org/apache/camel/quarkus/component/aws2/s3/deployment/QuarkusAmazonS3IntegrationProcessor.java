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
package org.apache.camel.quarkus.component.aws2.s3.deployment;

import io.quarkus.amazon.common.deployment.RequireAmazonClientInjectionBuildItem;
import io.quarkus.amazon.common.runtime.ClientUtil;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import org.jboss.jandex.DotName;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

public class QuarkusAmazonS3IntegrationProcessor {

    private static final DotName S3_CLIENT = DotName.createSimple(S3Client.class);
    private static final DotName S3_PRESIGNER = DotName.createSimple(S3Presigner.class);

    @BuildStep
    void integrate(BuildProducer<RequireAmazonClientInjectionBuildItem> requireClientInjectionProducer) {

        // request a default bean for S3Client and S3Presigner.
        requireClientInjectionProducer.produce(new RequireAmazonClientInjectionBuildItem(
                S3_CLIENT, ClientUtil.DEFAULT_CLIENT_NAME));
        requireClientInjectionProducer.produce(new RequireAmazonClientInjectionBuildItem(
                S3_PRESIGNER, ClientUtil.DEFAULT_CLIENT_NAME));
    }
}
