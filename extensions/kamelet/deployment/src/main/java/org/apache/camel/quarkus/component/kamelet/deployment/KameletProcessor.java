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
package org.apache.camel.quarkus.component.kamelet.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.Ordered;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.RouteTemplateDefinition;
import org.apache.camel.quarkus.component.kamelet.KameletConfiguration;
import org.apache.camel.quarkus.component.kamelet.KameletRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelContextCustomizerBuildItem;
import org.apache.camel.spi.Resource;
import org.apache.camel.support.PluginHelper;

class KameletProcessor {
    private static final String FEATURE = "camel-kamelet";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    KameletResolverBuildItem defaultResolver() {
        return new KameletResolverBuildItem(new KameletResolver() {
            @Override
            public Optional<Resource> resolve(String id, CamelContext context) throws Exception {
                return Optional.ofNullable(
                        PluginHelper.getResourceLoader(context).resolveResource("/kamelets/" + id + ".kamelet.yaml"));
            }
        });
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelContextCustomizerBuildItem configureTemplates(
            List<KameletResolverBuildItem> resolvers,
            KameletConfiguration configuration,
            KameletRecorder recorder) throws Exception {

        List<RouteTemplateDefinition> definitions = new ArrayList<>();
        List<KameletResolver> kameletResolvers = resolvers.stream()
                .map(KameletResolverBuildItem::getResolver)
                .sorted(Comparator.comparingInt(Ordered::getOrder))
                .collect(Collectors.toList());

        CamelContext context = new DefaultCamelContext();
        ExtendedCamelContext ecc = context.getCamelContextExtension();

        for (String id : configuration.identifiers.orElse(Collections.emptyList())) {
            for (KameletResolver resolver : kameletResolvers) {
                final Optional<Resource> resource = resolver.resolve(id, context);
                if (!resource.isPresent()) {
                    continue;
                }

                Collection<RoutesBuilder> rbs = PluginHelper.getRoutesLoader(ecc).findRoutesBuilders(resource.get());
                for (RoutesBuilder rb : rbs) {
                    RouteBuilder routeBuilder = (RouteBuilder) rb;
                    routeBuilder.configure();
                    if (routeBuilder.getRouteTemplateCollection().getRouteTemplates().size() == 0) {
                        throw new IllegalStateException(
                                "No kamelet template was created for "
                                        + "kamelet:" + id + ". It might be that the kamelet was malformed?");
                    } else if (routeBuilder.getRouteTemplateCollection().getRouteTemplates().size() > 1) {
                        throw new IllegalStateException(
                                "A kamelet is not supposed to create more than one route ("
                                        + "kamelet:" + id + ","
                                        + "routes: " + routeBuilder.getRouteTemplateCollection().getRouteTemplates().size()
                                        + ")");
                    }

                    definitions.add(routeBuilder.getRouteTemplateCollection().getRouteTemplates().get(0));
                }
            }
        }

        return new CamelContextCustomizerBuildItem(recorder.createTemplateLoaderCustomizer(definitions));
    }
}
