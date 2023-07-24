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
package org.apache.camel.quarkus.k.support;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.quarkus.k.core.SourceDefinition;
import org.apache.camel.quarkus.k.listener.SourcesConfigurer;
import org.junit.jupiter.api.Test;

import static org.apache.camel.util.PropertiesHelper.asProperties;
import static org.assertj.core.api.Assertions.assertThat;

public class PropertiesSupportTest {
    @Test
    public void propertiesAreBoundToSourcesConfigurer() {
        CamelContext context = new DefaultCamelContext();
        context.getPropertiesComponent().setInitialProperties(asProperties(
                "camel.k.sources[0].name", "MyRoutesWithBeans",
                "camel.k.sources[0].location", "classpath:MyRoutesWithBeans.java",
                "camel.k.sources[1].name", "MyRoutesConfig",
                "camel.k.sources[1].location", "classpath:MyRoutesConfig.java",
                "camel.k.sources[1].property-names[0]", "foo",
                "camel.k.sources[1].property-names[1]", "bar"));

        SourcesConfigurer configuration = new SourcesConfigurer();

        PropertiesSupport.bindProperties(
                context,
                configuration,
                k -> k.startsWith(SourcesConfigurer.CAMEL_K_SOURCES_PREFIX),
                SourcesConfigurer.CAMEL_K_PREFIX);

        assertThat(configuration.getSources())
                .hasSize(2)
                .anyMatch(byNameAndLocation("MyRoutesWithBeans", "classpath:MyRoutesWithBeans.java")
                        .and(d -> d.getPropertyNames() == null))
                .anyMatch(byNameAndLocation("MyRoutesConfig", "classpath:MyRoutesConfig.java")
                        .and(d -> d.getPropertyNames() != null && d.getPropertyNames().containsAll(List.of("foo", "bar"))));
    }

    @Test
    public void propertiesWithGapsAreBoundToSourcesConfigurer() {
        CamelContext context = new DefaultCamelContext();
        context.getPropertiesComponent().setInitialProperties(asProperties(
                "camel.k.sources[0].name", "MyRoutesWithBeans",
                "camel.k.sources[0].location", "classpath:MyRoutesWithBeans.java",
                "camel.k.sources[2].name", "MyRoutesConfig",
                "camel.k.sources[2].location", "classpath:MyRoutesConfig.java"));

        SourcesConfigurer configuration = new SourcesConfigurer();

        PropertiesSupport.bindProperties(
                context,
                configuration,
                k -> k.startsWith(SourcesConfigurer.CAMEL_K_SOURCES_PREFIX),
                SourcesConfigurer.CAMEL_K_PREFIX);

        assertThat(configuration.getSources())
                .hasSize(3)
                .filteredOn(Objects::nonNull)
                .hasSize(2)
                .anyMatch(byNameAndLocation("MyRoutesWithBeans", "classpath:MyRoutesWithBeans.java"))
                .anyMatch(byNameAndLocation("MyRoutesConfig", "classpath:MyRoutesConfig.java"));
    }

    // ***************************
    //
    // Helpers
    //
    // ***************************

    private static Predicate<SourceDefinition> byNameAndLocation(String name, String location) {
        return def -> Objects.equals(def.getName(), name) && Objects.equals(def.getLocation(), location);
    }
}
