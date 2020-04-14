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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.inject.spi.DeploymentException;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import org.jboss.jandex.DotName;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpService;

class Aws2S3Processor {

    private static final String FEATURE = "camel-aws2-s3";

    public static final String AWS_SDK_APPLICATION_ARCHIVE_MARKERS = "software/amazon/awssdk";

    private static final String APACHE_HTTP_SERVICE = "software.amazon.awssdk.http.apache.ApacheSdkHttpService";

    private static final List<String> INTERCEPTOR_PATHS = Arrays.asList(
            "software/amazon/awssdk/global/handlers/execution.interceptors",
            "software/amazon/awssdk/services/s3/execution.interceptors");

    private static final DotName EXECUTION_INTERCEPTOR_NAME = DotName.createSimple(ExecutionInterceptor.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep(applicationArchiveMarkers = { AWS_SDK_APPLICATION_ARCHIVE_MARKERS })
    void process(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<NativeImageResourceBuildItem> resource) {

        INTERCEPTOR_PATHS.forEach(path -> resource.produce(new NativeImageResourceBuildItem(path)));

        List<String> knownInterceptorImpls = combinedIndexBuildItem.getIndex()
                .getAllKnownImplementors(EXECUTION_INTERCEPTOR_NAME)
                .stream()
                .map(c -> c.name().toString()).collect(Collectors.toList());

        reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false,
                knownInterceptorImpls.toArray(new String[knownInterceptorImpls.size()])));

        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false,
                String.class.getCanonicalName()));
    }

    @BuildStep(loadsApplicationClasses = true)
    void client(BeanRegistrationPhaseBuildItem beanRegistrationPhase,
            BuildProducer<ServiceProviderBuildItem> serviceProvider,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxyDefinition) {
        checkClasspath(APACHE_HTTP_SERVICE, "apache-client");

        //Register Apache client as sync client
        proxyDefinition.produce(
                new NativeImageProxyDefinitionBuildItem("org.apache.http.conn.HttpClientConnectionManager",
                        "org.apache.http.pool.ConnPoolControl",
                        "software.amazon.awssdk.http.apache.internal.conn.Wrapped"));

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
