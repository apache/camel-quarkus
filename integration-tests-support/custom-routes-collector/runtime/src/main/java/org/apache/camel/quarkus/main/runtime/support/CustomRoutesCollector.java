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
package org.apache.camel.quarkus.main.runtime.support;

import java.util.Collections;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.main.RoutesCollector;
import org.apache.camel.model.RouteTemplatesDefinition;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.model.rest.RestsDefinition;

public class CustomRoutesCollector implements RoutesCollector {
    @Override
    public List<RoutesBuilder> collectRoutesFromRegistry(
            CamelContext camelContext,
            String excludePattern,
            String includePattern) {
        return Collections.emptyList();
    }

    @Override
    public List<RoutesDefinition> collectXmlRoutesFromDirectory(CamelContext camelContext, String directory) throws Exception {
        return Collections.emptyList();
    }

    @Override
    public List<RouteTemplatesDefinition> collectXmlRouteTemplatesFromDirectory(CamelContext camelContext, String directory)
            throws Exception {
        return Collections.emptyList();
    }

    @Override
    public List<RestsDefinition> collectXmlRestsFromDirectory(CamelContext camelContext, String directory) throws Exception {
        return Collections.emptyList();
    }
}
