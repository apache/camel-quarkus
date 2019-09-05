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
package org.apache.camel.quarkus.component.bean.deployment;

import java.util.List;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.model.BeanDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.quarkus.core.deployment.CamelRegistryBuildItem;
import org.apache.camel.quarkus.core.deployment.RouteBuilderBuildItem;
import org.apache.camel.quarkus.core.runtime.support.FastCamelContext;
import org.apache.camel.quarkus.core.runtime.support.FastModel;
import org.apache.camel.quarkus.core.runtime.support.RuntimeRegistry;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;

class BeanProcessor {

    private static final String FEATURE = "camel-bean";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void registerForReflection(List<RouteBuilderBuildItem> buildTimeRouteBuilderBuildItems, List<CamelRegistryBuildItem> registryItems, BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer) {
        if (!buildTimeRouteBuilderBuildItems.isEmpty()) {
            final RuntimeRegistry registry = new RuntimeRegistry();
            final FastCamelContext ctx = new FastCamelContext();
            ctx.setRegistry(registry);
            final FastModel model = new FastModel(ctx);
            ctx.setModel(model);

            for (RouteBuilderBuildItem i : buildTimeRouteBuilderBuildItems) {
                try {
                    final Class<?> cl = Class.forName(i.getClassName());
                    final RoutesBuilder rb = (RoutesBuilder) cl.newInstance();
                    rb.addRoutesToCamelContext(ctx);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            for (RouteDefinition rd : model.getRouteDefinitions()) {
                for (ProcessorDefinition<?> pd : rd.getOutputs()) {
                    if (pd instanceof BeanDefinition) {
                        final BeanDefinition bd = (BeanDefinition) pd;
                        if (bd.getBean() != null) {
                            reflectiveClassProducer.produce(new ReflectiveClassBuildItem(true, true, bd.getBean().getClass()));
                        } else if (bd.getBeanClass() != null) {
                            reflectiveClassProducer.produce(new ReflectiveClassBuildItem(true, true, bd.getBeanClass()));
                        } else if (bd.getBeanType() != null) {
                            reflectiveClassProducer.produce(new ReflectiveClassBuildItem(true, true, bd.getBeanType()));
                        }
                    }
                }
            }
        }
    }

}
