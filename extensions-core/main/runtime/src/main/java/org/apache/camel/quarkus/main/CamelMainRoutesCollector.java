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
package org.apache.camel.quarkus.main;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.main.DefaultRoutesCollector;
import org.apache.camel.model.rest.RestsDefinition;
import org.apache.camel.quarkus.core.RegistryRoutesLoader;
import org.apache.camel.spi.PackageScanResourceResolver;
import org.apache.camel.util.IOHelper;
import org.apache.camel.util.StopWatch;

public class CamelMainRoutesCollector extends DefaultRoutesCollector {
    private final RegistryRoutesLoader registryRoutesLoader;

    public CamelMainRoutesCollector(RegistryRoutesLoader registryRoutesLoader) {
        this.registryRoutesLoader = registryRoutesLoader;
    }

    public RegistryRoutesLoader getRegistryRoutesLoader() {
        return registryRoutesLoader;
    }

    @Override
    public List<RoutesBuilder> collectRoutesFromRegistry(
            CamelContext camelContext,
            String excludePattern,
            String includePattern) {

        return registryRoutesLoader.collectRoutesFromRegistry(camelContext, excludePattern, includePattern);
    }

    /**
     * TODO: Remove this when upgrading to Camel > 3.5.0.
     *
     * https://github.com/apache/camel-quarkus/issues/1852
     */
    @Override
    public List<RestsDefinition> collectXmlRestsFromDirectory(CamelContext camelContext, String directory) {
        ExtendedCamelContext ecc = camelContext.adapt(ExtendedCamelContext.class);
        PackageScanResourceResolver resolver = camelContext.adapt(ExtendedCamelContext.class).getPackageScanResourceResolver();

        List<RestsDefinition> answer = new ArrayList<>();

        StopWatch watch = new StopWatch();
        int count = 0;
        String[] parts = directory.split(",");
        for (String part : parts) {
            log.debug("Loading additional Camel XML rests from: {}", part);
            try {
                Set<InputStream> set = resolver.findResources(part);
                for (InputStream is : set) {
                    log.debug("Found XML rest from location: {}", part);
                    RestsDefinition rests = (RestsDefinition) ecc.getXMLRoutesDefinitionLoader().loadRestsDefinition(ecc, is);
                    if (rests != null) {
                        answer.add(rests);
                        IOHelper.close(is);
                        count += rests.getRests().size();
                    }
                }
            } catch (FileNotFoundException e) {
                log.debug("No XML rests found in {}. Skipping XML rests detection.", part);
            } catch (Exception e) {
                throw RuntimeCamelException.wrapRuntimeException(e);
            }
            if (count > 0) {
                log.info("Loaded {} ({} millis) additional Camel XML rests from: {}", count, watch.taken(), directory);
            } else {
                log.info("No additional Camel XML rests discovered from: {}", directory);
            }
        }

        return answer;
    }
}
