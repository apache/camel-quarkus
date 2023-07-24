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
package org.apache.camel.quarkus.k.runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.smallrye.config.PropertiesConfigSource;
import org.apache.camel.component.kubernetes.properties.ConfigMapPropertiesFunction;
import org.apache.camel.quarkus.k.support.RuntimeSupport;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

public class ApplicationConfigSourceProvider implements ConfigSourceProvider {

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        final Map<String, String> sysProperties = new HashMap<>();
        // explicit disable looking up configmap and secret using the KubernetesClient
        sysProperties.put(ConfigMapPropertiesFunction.CLIENT_ENABLED, "false");

        final Map<String, String> appProperties = RuntimeSupport.loadApplicationProperties();
        final Map<String, String> usrProperties = RuntimeSupport.loadUserProperties();

        return List.of(
                new PropertiesConfigSource(sysProperties, "camel-k-sys", ConfigSource.DEFAULT_ORDINAL + 1000),
                new PropertiesConfigSource(appProperties, "camel-k-app", ConfigSource.DEFAULT_ORDINAL),
                new PropertiesConfigSource(usrProperties, "camel-k-usr", ConfigSource.DEFAULT_ORDINAL + 1));
    }
}
