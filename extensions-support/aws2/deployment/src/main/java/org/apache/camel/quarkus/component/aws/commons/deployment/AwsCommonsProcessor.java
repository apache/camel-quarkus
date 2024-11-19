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
package org.apache.camel.quarkus.component.aws.commons.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import jakarta.enterprise.inject.spi.DeploymentException;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.pool.ConnPoolControl;
import software.amazon.awssdk.http.SdkHttpService;
import software.amazon.awssdk.http.apache.internal.conn.Wrapped;

class AwsCommonsProcessor {
    private static final String FEATURE = "camel-aws2-commons";
    private static final String APACHE_HTTP_SERVICE = "software.amazon.awssdk.http.apache.ApacheSdkHttpService";

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    NativeImageProxyDefinitionBuildItem httpProxies() {
        return new NativeImageProxyDefinitionBuildItem(HttpClientConnectionManager.class.getName(),
                ConnPoolControl.class.getName(), Wrapped.class.getName());
    }

    @BuildStep
    void client(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        checkClasspath(APACHE_HTTP_SERVICE, "apache-client");
        serviceProvider.produce(new ServiceProviderBuildItem(SdkHttpService.class.getName(), APACHE_HTTP_SERVICE));
    }

    private void checkClasspath(String className, String dependencyName) {
        try {
            Class.forName(className, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new DeploymentException(
                    "Missing 'software.amazon.awssdk:" + dependencyName + "' dependency on the classpath");
        }
    }
}
