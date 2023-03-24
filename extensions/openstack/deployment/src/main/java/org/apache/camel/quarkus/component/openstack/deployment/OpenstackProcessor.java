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
package org.apache.camel.quarkus.component.openstack.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import org.apache.camel.quarkus.core.deployment.util.CamelSupport;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;
import org.openstack4j.connectors.okhttp.HttpExecutorServiceImpl;
import org.openstack4j.core.transport.HttpExecutorService;
import org.openstack4j.model.ModelEntity;
import org.openstack4j.openstack.identity.v3.domain.KeystoneAuth;

class OpenstackProcessor {
    private static final Logger LOG = Logger.getLogger(OpenstackProcessor.class);

    private static final String FEATURE = "camel-openstack";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    RuntimeInitializedClassBuildItem runtimeInitOpenstack4jUntrustedSSL() {
        // This class uses a SecureRandom which needs to be initialized at run time
        return new RuntimeInitializedClassBuildItem("org.openstack4j.core.transport.UntrustedSSL");
    }

    @BuildStep
    void registerOkhttpServiceProvider(BuildProducer<ServiceProviderBuildItem> services) {
        services.produce(
                new ServiceProviderBuildItem(HttpExecutorService.class.getName(), HttpExecutorServiceImpl.class.getName()));
    }

    @BuildStep
    void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        // Register ModelEntity implementations for reflection
        index.getAllKnownImplementors(DotName.createSimple(ModelEntity.class.getName())).stream()
                .filter(CamelSupport::isConcrete).forEach(ci -> {
                    String className = ci.asClass().name().toString();
                    LOG.debugf("Registered openstack4j model class %s as reflective", className);
                    reflectiveClasses.produce(ReflectiveClassBuildItem.builder(className).methods().fields().build());
                });

        // Some ModelEntity sub-interfaces embed nested interfaces that are not themselves ModelEntity, so registering manually
        reflectiveClasses
                .produce(ReflectiveClassBuildItem.builder(KeystoneAuth.AuthIdentity.class).methods().fields().build());
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(KeystoneAuth.AuthIdentity.AuthPassword.class).methods()
                .fields().build());
        reflectiveClasses.produce(
                ReflectiveClassBuildItem.builder(KeystoneAuth.AuthIdentity.AuthToken.class).methods().fields().build());
        reflectiveClasses
                .produce(ReflectiveClassBuildItem.builder(KeystoneAuth.AuthScope.class).methods().fields().build());

        // Register open stack services for reflection
        BuildTimeDefaultAPIProvider prov = new BuildTimeDefaultAPIProvider();
        reflectiveClasses
                .produce(ReflectiveClassBuildItem.builder(prov.getReflectiveClasses()).build());
    }

    @BuildStep
    void addDependencies(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("com.github.openstack4j.core", "openstack4j-core"));
    }

}
