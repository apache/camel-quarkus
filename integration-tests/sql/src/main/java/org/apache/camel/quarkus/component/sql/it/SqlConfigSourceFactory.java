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
package org.apache.camel.quarkus.component.sql.it;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.common.MapBackedConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSource;

public class SqlConfigSourceFactory implements ConfigSourceFactory {

    private static MapBackedConfigSource source;

    static {
        String jdbcUrl = System.getenv("SQL_JDBC_URL");

        Map<String, String> props = new HashMap();
        //external db
        if (jdbcUrl != null) {
            props.put("quarkus.datasource.jdbc.url", jdbcUrl);
            props.put("quarkus.datasource.username", System.getenv("SQL_JDBC_USERNAME"));
            props.put("quarkus.datasource.password", System.getenv("SQL_JDBC_PASSWORD"));
        } else {
            //derby could be started in container
            boolean useDocker = Boolean.parseBoolean(System.getenv("SQL_USE_DERBY_DOCKER")) &&
                    "derby".equals(System.getProperty("cq.sqlJdbcKind"));
            props.put("quarkus.devservices.enabled", String.valueOf(!useDocker));
        }

        source = new MapBackedConfigSource("env_database", props) {
        };
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(ConfigSourceContext configSourceContext) {
        return Collections.singletonList(source);
    }

    @Override
    public OptionalInt getPriority() {
        return OptionalInt.of(999);
    }

}
