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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.apache.camel.Ordered;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dsl.yaml.YamlRoutesBuilderLoader;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.RouteTemplateDefinition;
import org.apache.camel.quarkus.component.kamelet.KameletConfiguration;
import org.apache.camel.quarkus.component.kamelet.KameletRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelContextCustomizerBuildItem;
import org.apache.camel.spi.Resource;
import org.apache.camel.support.ResourceHelper;

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
            public Optional<InputStream> resolve(String id) throws Exception {
                return Optional.ofNullable(
                        Thread.currentThread().getContextClassLoader()
                                .getResourceAsStream("/kamelets/" + id + ".kamelet.yaml"));
            }
        });
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelContextCustomizerBuildItem configureTemplates(
            List<KameletResolverBuildItem> resolvers,
            KameletConfiguration configuration,
            KameletRecorder recorder) throws Exception {

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        List<RouteTemplateDefinition> definitions = new ArrayList<>();
        List<KameletResolver> kameletResolvers = resolvers.stream()
                .map(KameletResolverBuildItem::getResolver)
                .sorted(Comparator.comparingInt(Ordered::getOrder))
                .collect(Collectors.toList());

        try (YamlRoutesBuilderLoader ybl = new YamlRoutesBuilderLoader()) {
            ybl.setCamelContext(new DefaultCamelContext());
            ybl.start();

            for (String name : configuration.names.orElse(Collections.emptyList())) {

                for (KameletResolver resolver : kameletResolvers) {
                    final Optional<InputStream> is = resolver.resolve(name);
                    if (!is.isPresent()) {
                        continue;
                    }

                    try {
                        final ObjectNode definition = (ObjectNode) mapper.readTree(is.get());
                        final JsonNode properties = definition.requiredAt("/spec/definition/properties");
                        final JsonNode flow = mapper.createArrayNode().add(definition.requiredAt("/spec/flow"));
                        final Resource res = ResourceHelper.fromBytes(name + ".yaml", mapper.writeValueAsBytes(flow));

                        RouteTemplateDefinition rt = new RouteTemplateDefinition();
                        rt.setId(name);

                        Iterator<Map.Entry<String, JsonNode>> it = properties.fields();
                        while (it.hasNext()) {
                            final Map.Entry<String, JsonNode> property = it.next();
                            final String key = property.getKey();
                            final JsonNode def = property.getValue().at("/default");

                            if (def.isMissingNode()) {
                                rt.templateParameter(key);
                            } else {
                                rt.templateParameter(key, def.asText());
                            }
                        }

                        RouteBuilder rb = (RouteBuilder) ybl.loadRoutesBuilder(res);
                        rb.configure();
                        if (rb.getRouteCollection().getRoutes().size() != 1) {
                            throw new IllegalStateException(
                                    "A kamelet is not supposed to create more than one route ("
                                            + "kamelet:" + name + ","
                                            + "routes: " + rb.getRouteCollection().getRoutes().size()
                                            + ")");
                        }

                        rt.setRoute(rb.getRouteCollection().getRoutes().get(0));

                        definitions.add(rt);
                    } finally {
                        is.get().close();
                    }
                }
            }
        }

        return new CamelContextCustomizerBuildItem(recorder.createTemplateLoaderCustomizer(definitions));
    }
}
