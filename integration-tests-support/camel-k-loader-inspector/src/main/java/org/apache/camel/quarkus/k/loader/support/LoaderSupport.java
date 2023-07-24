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
package org.apache.camel.quarkus.k.loader.support;

import java.nio.charset.StandardCharsets;
import java.util.Collection;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.quarkus.k.core.Runtime;
import org.apache.camel.spi.RoutesLoader;
import org.apache.camel.support.PluginHelper;
import org.apache.camel.support.ResourceHelper;

public final class LoaderSupport {
    private LoaderSupport() {
    }

    public static JsonObject inspectSource(CamelContext context, String location, byte[] code) throws Exception {
        final Runtime runtime = Runtime.on(context);
        final ExtendedCamelContext ecc = runtime.getExtendedCamelContext();
        final RoutesLoader loader = PluginHelper.getRoutesLoader(ecc);
        final Collection<RoutesBuilder> builders = loader.findRoutesBuilders(ResourceHelper.fromBytes(location, code));

        for (RoutesBuilder builder : builders) {
            runtime.addRoutes(builder);
        }

        return Json.createObjectBuilder()
                .add("components", extractComponents(context))
                .add("routes", extractRoutes(context))
                .add("endpoints", extractEndpoints(context))
                .build();
    }

    public static JsonObject inspectSource(CamelContext context, String location, String code) throws Exception {
        return inspectSource(context, location, code.getBytes(StandardCharsets.UTF_8));
    }

    public static JsonArrayBuilder extractComponents(CamelContext context) {
        JsonArrayBuilder answer = Json.createArrayBuilder();
        context.getComponentNames().forEach(answer::add);

        return answer;
    }

    public static JsonArrayBuilder extractRoutes(CamelContext context) {
        JsonArrayBuilder answer = Json.createArrayBuilder();
        context.getRoutes().forEach(r -> answer.add(r.getId()));

        return answer;
    }

    public static JsonArrayBuilder extractEndpoints(CamelContext context) {
        JsonArrayBuilder answer = Json.createArrayBuilder();
        context.getEndpoints().forEach(e -> answer.add(e.getEndpointUri()));

        return answer;
    }
}
