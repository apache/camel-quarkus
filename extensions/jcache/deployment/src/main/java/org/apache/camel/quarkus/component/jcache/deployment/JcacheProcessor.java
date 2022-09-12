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
package org.apache.camel.quarkus.component.jcache.deployment;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Stream;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import org.jboss.logging.Logger;

class JcacheProcessor {

    private static final Logger LOG = Logger.getLogger(JcacheProcessor.class);
    private static final String FEATURE = "camel-jcache";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void nativeResources(BuildProducer<ServiceProviderBuildItem> services,
            BuildProducer<NativeImageResourceBuildItem> nativeResources) {
        Stream.of("javax.cache.spi.CachingProvider")
                .forEach(service -> {
                    try {
                        Set<String> implementations = ServiceUtil.classNamesNamedIn(
                                Thread.currentThread().getContextClassLoader(),
                                "META-INF/services/" + service);
                        services.produce(
                                new ServiceProviderBuildItem(service, implementations));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
