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
package org.apache.camel.quarkus.component.kamelet;

import java.util.List;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.annotations.RelaxedValidation;
import org.apache.camel.CamelContext;
import org.apache.camel.model.Model;
import org.apache.camel.model.RouteTemplateDefinition;
import org.apache.camel.spi.CamelContextCustomizer;
import org.apache.camel.spi.Resource;
import org.apache.camel.spi.ResourceLoader;
import org.apache.camel.support.PluginHelper;
import org.apache.camel.util.ObjectHelper;
import org.jboss.logging.Logger;

@Recorder
public class KameletRecorder {
    private static final Logger LOG = Logger.getLogger(KameletRecorder.class);

    public RuntimeValue<CamelContextCustomizer> createTemplateLoaderCustomizer(
            @RelaxedValidation List<RouteTemplateDefinition> definitions) {

        return new RuntimeValue<>(new CamelContextCustomizer() {
            @Override
            public void configure(CamelContext context) {
                try {
                    // TODO: Improve / remove this: https://github.com/apache/camel-quarkus/issues/5230
                    ResourceLoader resourceLoader = PluginHelper.getResourceLoader(context);
                    for (RouteTemplateDefinition definition : definitions) {
                        Resource originalResource = definition.getResource();
                        String location = originalResource.getLocation();
                        if (originalResource instanceof EmptyKameletResource && !ObjectHelper.isNotEmpty(location)) {
                            Resource resource = resourceLoader.resolveResource(location);
                            if (resource != null) {
                                definition.setResource(resource);
                            } else {
                                if (LOG.isDebugEnabled()) {
                                    LOG.debugf("Failed resolving Kamelet resource %s. Resource dumping will be disabled.",
                                            definition.getId());
                                }
                            }
                        }
                    }
                    context.getCamelContextExtension().getContextPlugin(Model.class).addRouteTemplateDefinitions(definitions);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
