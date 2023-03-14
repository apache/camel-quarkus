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
package org.apache.camel.quarkus.component.braintree.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.component.braintree.BraintreeComponent;
import org.apache.camel.quarkus.component.braintree.graal.BraintreeRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelBeanBuildItem;

class BraintreeProcessor {

    private static final String FEATURE = "camel-braintree";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, CombinedIndexBuildItem combinedIndex) {
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("com.braintreegateway.Address",
                "com.braintreegateway.BraintreeGateway",
                "com.braintreegateway.Customer",
                "com.braintreegateway.DisputeEvidence",
                "com.braintreegateway.DocumentUpload",
                "com.braintreegateway.MerchantAccount",
                "com.braintreegateway.PaymentMethod",
                "com.braintreegateway.Transaction").methods(true).fields(false).build());
    }

    @BuildStep
    void nativeImageResources(BuildProducer<NativeImageResourceBuildItem> nativeImage) {
        nativeImage.produce(new NativeImageResourceBuildItem(
                "ssl/api_braintreegateway_com.ca.crt",
                "ssl/payments_braintreeapi_com.ca.crt"));
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelBeanBuildItem configureBraintreeComponent(BraintreeRecorder recorder) {
        return new CamelBeanBuildItem("braintree", BraintreeComponent.class.getName(),
                recorder.configureBraintreeComponent());
    }
}
