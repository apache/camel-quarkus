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
package org.apache.camel.quarkus.component.graphql;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "camel.graphql", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class CamelGraphQLConfig {

    /**
     * A comma separated list of paths to files containing GraphQL queries for use by GraphQL endpoints.
     *
     * Query files that only need to be accessible from the classpath should be specified on this property.
     *
     * Paths can either be schemeless (E.g graphql/my-query.graphql) or be prefixed with the classpath: URI scheme (E.g
     * classpath:graphql/my-query.graphql). Other URI schemes are not supported.
     */
    @ConfigItem
    public Optional<List<String>> queryFiles;
}
