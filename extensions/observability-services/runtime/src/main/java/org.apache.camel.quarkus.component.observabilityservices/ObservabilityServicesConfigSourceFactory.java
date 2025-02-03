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
package org.apache.camel.quarkus.component.observabilityservices;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.PropertiesConfigSource;
import org.apache.camel.util.ObjectHelper;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * TODO: Remove this https://github.com/apache/camel-quarkus/issues/6967
 */
public class ObservabilityServicesConfigSourceFactory implements ConfigSourceFactory {
    @Override
    public Iterable<ConfigSource> getConfigSources(ConfigSourceContext context) {
        ConfigValue sdkDisabled = context.getValue("quarkus.otel.sdk.disabled");

        if (sdkDisabled != null && sdkDisabled.getValue().equals("false")) {
            Map<String, String> properties = new HashMap<>(1);
            ConfigValue otelSuppressedUris = context.getValue("quarkus.otel.traces.suppress-application-uris");
            String suppressedEndpoints = "/observe/*";

            if (otelSuppressedUris != null && ObjectHelper.isNotEmpty(otelSuppressedUris.getValue())) {
                suppressedEndpoints += "," + otelSuppressedUris.getValue();
            }

            properties.put("quarkus.otel.traces.suppress-application-uris", suppressedEndpoints);
            return Set.of(new PropertiesConfigSource(properties, "camel-quarkus-observability-services", 300));
        }

        return Collections.emptySet();
    }
}
