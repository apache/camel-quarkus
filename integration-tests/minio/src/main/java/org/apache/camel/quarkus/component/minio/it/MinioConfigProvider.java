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
package org.apache.camel.quarkus.component.minio.it;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

public class MinioConfigProvider implements ConfigSourceProvider {

    private final MinioConfig minioConfig = new MinioConfig();

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        return Collections.singletonList(minioConfig);
    }

    private static final class MinioConfig implements ConfigSource {

        private final Map<String, String> values = new HashMap<String, String>() {
            {
                put("quarkus.minio.url", String.format("http://%s:%s", System.getProperty(MinioResource.PARAM_SERVER_HOST),
                        System.getProperty(MinioResource.PARAM_SERVER_PORT)));
                put("quarkus.minio.access-key", MinioResource.SERVER_ACCESS_KEY);
                put("quarkus.minio.secret-key", MinioResource.SERVER_SECRET_KEY);
            }
        };

        @Override
        public Map<String, String> getProperties() {
            return values;
        }

        @Override
        public String getValue(String propertyName) {
            return values.get(propertyName);
        }

        @Override
        public String getName() {
            return MinioConfig.class.getName();
        }

        @Override
        public Set<String> getPropertyNames() {
            return values.keySet();
        }
    }
}
