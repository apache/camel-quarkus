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
package org.apache.camel.quarkus.k.listener;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.quarkus.k.core.SourceType;
import org.apache.camel.quarkus.k.support.PropertiesSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.apache.camel.util.PropertiesHelper.asProperties;
import static org.assertj.core.api.Assertions.assertThat;

public class SourceConfigurerTest {
    @Test
    public void shouldLoadMultipleSources() {
        CamelContext context = new DefaultCamelContext();
        context.getPropertiesComponent().setInitialProperties(asProperties(
                "camel.k.sources[0].name", "templateName",
                "camel.k.sources[0].location", "classpath:MyTemplate.java",
                "camel.k.sources[0].type", "template",
                "camel.k.sources[1].name", "src",
                "camel.k.sources[1].location", "classpath:MySrc.java",
                "camel.k.sources[2].name", "err",
                "camel.k.sources[2].location", "classpath:Err.java",
                "camel.k.sources[2].type", "errorHandler"));

        SourcesConfigurer configuration = new SourcesConfigurer();

        PropertiesSupport.bindProperties(
                context,
                configuration,
                k -> k.startsWith(SourcesConfigurer.CAMEL_K_SOURCES_PREFIX),
                SourcesConfigurer.CAMEL_K_PREFIX);

        assertThat(configuration.getSources()).hasSize(3);
    }

    @Test
    public void shouldFailOnMultipleErrorHandlers() {
        CamelContext context = new DefaultCamelContext();
        context.getPropertiesComponent().setInitialProperties(asProperties(
                "camel.k.sources[0].name", "templateName0",
                "camel.k.sources[0].location", "classpath:MyTemplate1.java",
                "camel.k.sources[0].type", "template",
                "camel.k.sources[1].name", "err1",
                "camel.k.sources[1].location", "classpath:Err1.java",
                "camel.k.sources[1].type", "errorHandler",
                "camel.k.sources[2].name", "err2",
                "camel.k.sources[2].location", "classpath:Err2.java",
                "camel.k.sources[2].type", "errorHandler"));

        SourcesConfigurer configuration = new SourcesConfigurer();

        PropertiesSupport.bindProperties(
                context,
                configuration,
                k -> k.startsWith(SourcesConfigurer.CAMEL_K_SOURCES_PREFIX),
                SourcesConfigurer.CAMEL_K_PREFIX);

        assertThat(configuration.getSources()).hasSize(3);
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            SourcesConfigurer.checkUniqueErrorHandler(configuration.getSources());
        }, "java.lang.IllegalArgumentException: Expected only one error handler source type, got 2");
    }

    @Test
    public void shouldDefaultSourcesWithEmptyType() {
        CamelContext context = new DefaultCamelContext();
        context.getPropertiesComponent().setInitialProperties(asProperties(
                "camel.k.sources[0].name", "source0",
                "camel.k.sources[1].name", "source1",
                "camel.k.sources[2].name", "source2",
                "camel.k.sources[2].type", "source"));

        SourcesConfigurer configuration = new SourcesConfigurer();

        PropertiesSupport.bindProperties(
                context,
                configuration,
                k -> k.startsWith(SourcesConfigurer.CAMEL_K_SOURCES_PREFIX),
                SourcesConfigurer.CAMEL_K_PREFIX);

        assertThat(configuration.getSources().length).isEqualTo(3);
        assertThat(configuration.getSources()[0].getType()).isEqualTo(SourceType.source);
        assertThat(configuration.getSources()[1].getType()).isEqualTo(SourceType.source);
        assertThat(configuration.getSources()[2].getType()).isEqualTo(SourceType.source);
    }

    @Test
    public void shouldOrderSourcesByType() {
        CamelContext context = new DefaultCamelContext();
        context.getPropertiesComponent().setInitialProperties(asProperties(
                "camel.k.sources[0].name", "template1",
                "camel.k.sources[0].type", "template",
                "camel.k.sources[1].name", "source1",
                "camel.k.sources[1].type", "source",
                "camel.k.sources[2].name", "source2",
                "camel.k.sources[3].name", "errorHandler1",
                "camel.k.sources[3].type", "errorHandler"));

        SourcesConfigurer configuration = new SourcesConfigurer();

        PropertiesSupport.bindProperties(
                context,
                configuration,
                k -> k.startsWith(SourcesConfigurer.CAMEL_K_SOURCES_PREFIX),
                SourcesConfigurer.CAMEL_K_PREFIX);
        SourcesConfigurer.sortSources(configuration.getSources());

        assertThat(configuration.getSources()).hasSize(4);
        assertThat(configuration.getSources()[0].getName()).isEqualTo("errorHandler1");
        assertThat(configuration.getSources()[0].getType()).isEqualTo(SourceType.errorHandler);
        assertThat(configuration.getSources()[1].getName()).isEqualTo("template1");
        assertThat(configuration.getSources()[1].getType()).isEqualTo(SourceType.template);
        // Order for the same type does not matter
        assertThat(configuration.getSources()[2].getName()).contains("source");
        assertThat(configuration.getSources()[2].getType()).isEqualTo(SourceType.source);
        assertThat(configuration.getSources()[3].getName()).contains("source");
        assertThat(configuration.getSources()[3].getType()).isEqualTo(SourceType.source);
    }

    @Test
    public void shouldNotFailOnEmptySources() {
        SourcesConfigurer.sortSources(null);
        SourcesConfigurer.checkUniqueErrorHandler(null);
    }
}
