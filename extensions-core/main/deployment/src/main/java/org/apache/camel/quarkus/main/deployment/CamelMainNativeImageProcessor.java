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
package org.apache.camel.quarkus.main.deployment;

import java.util.stream.Collectors;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.quarkus.support.common.CamelCapabilities;
import org.apache.camel.support.ResourceHelper;
import org.apache.camel.util.AntPathMatcher;
import org.jboss.logging.Logger;

public class CamelMainNativeImageProcessor {
    private static final Logger LOG = Logger.getLogger(CamelMainNativeImageProcessor.class);

    @BuildStep
    ReflectiveClassBuildItem reflectiveCLasses() {
        // TODO: The classes below are needed to fix https://github.com/apache/camel-quarkus/issues/1005
        //       but we need to investigate why it does not fail with Java 1.8
        return new ReflectiveClassBuildItem(
                true,
                false,
                org.apache.camel.main.Resilience4jConfigurationProperties.class,
                org.apache.camel.model.Resilience4jConfigurationDefinition.class,
                org.apache.camel.model.Resilience4jConfigurationCommon.class,
                org.apache.camel.spi.RestConfiguration.class,
                org.apache.camel.quarkus.main.CamelMainApplication.class);
    }

    @BuildStep
    private void camelNativeImageResources(
            Capabilities capabilities,
            BuildProducer<NativeImageResourceBuildItem> nativeResource) {

        if (!capabilities.isCapabilityPresent(CamelCapabilities.XML)) {
            return;
        }

        for (String path : CamelMainHelper.routesIncludePatter().collect(Collectors.toList())) {
            String scheme = ResourceHelper.getScheme(path);

            // Null scheme is equivalent to classpath scheme
            if (scheme == null || scheme.equals("classpath:")) {
                if (AntPathMatcher.INSTANCE.isPattern(path)) {
                    // Classpath directory traversal via wildcard paths does not work on GraalVM.
                    // The exact path to the resource has to be looked up
                    // https://github.com/oracle/graal/issues/1108
                    LOG.warnf("Classpath wildcards does not work in native mode. Resources matching %s will not be loaded.",
                            path);
                } else {
                    nativeResource.produce(new NativeImageResourceBuildItem(path.replace("classpath:", "")));
                }
            }
        }
    }
}
