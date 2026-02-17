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
package org.apache.camel.quarkus.component.infinispan.cluster;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Runtime configuration options for Infinispan Cluster Service.
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.camel.cluster.infinispan")
public interface InfinispanClusterServiceRuntimeConfig {
    /**
     * The cluster service ID.
     */
    Optional<String> id();

    /**
     * The service lookup order/priority.
     */
    Optional<Integer> order();

    /**
     * The custom attributes associated to the service.
     */
    Map<String, String> attributes();

    /**
     * The Infinispan configuration URI. Can be used to specify a custom Infinispan configuration file.
     */
    Optional<String> configurationUri();

    /**
     * The Infinispan server hosts (e.g., "localhost:11222").
     */
    Optional<String> hosts();

    /**
     * Enable secure connections to an Infinispan server.
     */
    Optional<Boolean> secure();

    /**
     * Username for authentication with the Infinispan server.
     */
    Optional<String> username();

    /**
     * Password for authentication with the Infinispan server.
     */
    Optional<String> password();

    /**
     * SASL mechanism for authentication (e.g., DIGEST-MD5, PLAIN, SCRAM-SHA-256).
     */
    Optional<String> saslMechanism();

    /**
     * Security realm for authentication.
     */
    Optional<String> securityRealm();

    /**
     * Security server name for authentication.
     */
    Optional<String> securityServerName();

    /**
     * Additional configuration properties for the Infinispan client.
     */
    Map<String, String> configurationProperties();

    /**
     * Lifespan for entries in the cluster view cache.
     */
    @WithDefault("30")
    Long lifespan();

    /**
     * Time unit for lifespan (e.g., SECONDS, MINUTES, HOURS).
     */
    @WithDefault("SECONDS")
    TimeUnit lifespanTimeUnit();
}
