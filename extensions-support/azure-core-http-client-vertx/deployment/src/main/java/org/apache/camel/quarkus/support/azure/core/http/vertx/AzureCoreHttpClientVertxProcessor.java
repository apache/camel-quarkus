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
package org.apache.camel.quarkus.support.azure.core.http.vertx;

import com.azure.core.http.vertx.VertxProvider;
import io.netty.handler.ssl.OpenSsl;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;

public class AzureCoreHttpClientVertxProcessor {

    @BuildStep
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses) {
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(OpenSsl.class.getName()));
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem("io.netty.internal.tcnative.SSL"));
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(
                "com.azure.core.http.vertx.VertxAsyncHttpClientProvider$GlobalVertxHttpClient"));
        runtimeInitializedClasses.produce(
                new RuntimeInitializedClassBuildItem("com.azure.core.http.vertx.VertxAsyncHttpClientBuilder$DefaultVertx"));
    }

    @BuildStep
    void registerServiceProviders(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        serviceProvider.produce(ServiceProviderBuildItem.allProvidersFromClassPath(VertxProvider.class.getName()));
    }
}
