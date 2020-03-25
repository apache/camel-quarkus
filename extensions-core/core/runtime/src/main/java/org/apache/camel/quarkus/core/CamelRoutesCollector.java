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
package org.apache.camel.quarkus.core;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.main.RoutesCollector;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.model.rest.RestsDefinition;
import org.apache.camel.spi.PackageScanResourceResolver;
import org.apache.camel.spi.XMLRoutesDefinitionLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CamelRoutesCollector implements RoutesCollector {
    private static final Logger LOGGER = LoggerFactory.getLogger(CamelRoutesCollector.class);

    private final RegistryRoutesLoader registryRoutesLoader;
    private final XMLRoutesDefinitionLoader xmlRoutesLoader;

    public CamelRoutesCollector(RegistryRoutesLoader registryRoutesLoader, XMLRoutesDefinitionLoader xmlRoutesLoader) {
        this.registryRoutesLoader = registryRoutesLoader;
        this.xmlRoutesLoader = xmlRoutesLoader;
    }

    public RegistryRoutesLoader getRegistryRoutesLoader() {
        return registryRoutesLoader;
    }

    public XMLRoutesDefinitionLoader getXmlRoutesLoader() {
        return xmlRoutesLoader;
    }

    @Override
    public List<RoutesBuilder> collectRoutesFromRegistry(
            CamelContext camelContext,
            String excludePattern,
            String includePattern) {

        return registryRoutesLoader.collectRoutesFromRegistry(camelContext, excludePattern, includePattern);
    }

    @Override
    public List<RoutesDefinition> collectXmlRoutesFromDirectory(CamelContext camelContext, String directory) {
        List<RoutesDefinition> answer = new ArrayList<>();
        PackageScanResourceResolver resolver = camelContext.adapt(ExtendedCamelContext.class).getPackageScanResourceResolver();

        for (String part : directory.split(",")) {
            LOGGER.info("Loading additional Camel XML routes from: {}", part);
            try {
                LOGGER.info("Resolving from current dir: {}", Paths.get(".").toAbsolutePath().toString());
                int res = 0;
                for (InputStream is : resolver.findResources(part)) {
                    res++;
                    Object definition = xmlRoutesLoader.loadRoutesDefinition(camelContext, is);
                    if (definition instanceof RoutesDefinition) {
                        answer.add((RoutesDefinition) definition);
                    }
                }
                LOGGER.info("Found {} resources", res);
            } catch (FileNotFoundException e) {
                LOGGER.debug("No XML routes found in {}. Skipping XML routes detection.", part);
            } catch (Exception e) {
                throw RuntimeCamelException.wrapRuntimeException(e);
            }
        }

        return answer;
    }

    @Override
    public List<RestsDefinition> collectXmlRestsFromDirectory(CamelContext camelContext, String directory) {
        List<RestsDefinition> answer = new ArrayList<>();
        PackageScanResourceResolver resolver = camelContext.adapt(ExtendedCamelContext.class).getPackageScanResourceResolver();

        for (String part : directory.split(",")) {
            LOGGER.info("Loading additional Camel XML rests from: {}", part);
            try {
                for (InputStream is : resolver.findResources(part)) {
                    Object definition = xmlRoutesLoader.loadRestsDefinition(camelContext, is);
                    if (definition instanceof RestsDefinition) {
                        answer.add((RestsDefinition) definition);
                    }
                }
            } catch (FileNotFoundException e) {
                LOGGER.debug("No XML rests found in {}. Skipping XML rests detection.", part);
            } catch (Exception e) {
                throw RuntimeCamelException.wrapRuntimeException(e);
            }
        }

        return answer;
    }
}
