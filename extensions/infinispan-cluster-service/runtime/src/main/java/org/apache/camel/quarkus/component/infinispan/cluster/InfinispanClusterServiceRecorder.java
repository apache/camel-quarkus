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

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.component.infinispan.cluster.InfinispanClusterService;
import org.apache.camel.component.infinispan.remote.cluster.InfinispanRemoteClusterService;
import org.infinispan.commons.marshall.ProtoStreamMarshaller;

@Recorder
public class InfinispanClusterServiceRecorder {
    private final RuntimeValue<InfinispanClusterServiceRuntimeConfig> config;

    public InfinispanClusterServiceRecorder(RuntimeValue<InfinispanClusterServiceRuntimeConfig> config) {
        this.config = config;
    }

    public RuntimeValue<InfinispanClusterService> createInfinispanClusterService() {
        InfinispanRemoteClusterService clusterService = new InfinispanRemoteClusterService();
        InfinispanClusterServiceRuntimeConfig config = this.config.getValue();
        config.id().ifPresent(clusterService::setId);
        config.order().ifPresent(clusterService::setOrder);
        config.configurationUri().ifPresent(clusterService::setConfigurationUri);
        config.hosts().ifPresent(clusterService::setHosts);
        config.secure().ifPresent(clusterService::setSecure);
        config.username().ifPresent(clusterService::setUsername);
        config.password().ifPresent(clusterService::setPassword);
        config.saslMechanism().ifPresent(clusterService::setSaslMechanism);
        config.securityRealm().ifPresent(clusterService::setSecurityRealm);
        config.securityServerName().ifPresent(clusterService::setSecurityServerName);
        clusterService.setLifespanTimeUnit(config.lifespanTimeUnit());
        clusterService.setLifespan(config.lifespan());
        config.attributes().forEach(clusterService::setAttribute);
        config.configurationProperties().putIfAbsent("infinispan.client.hotrod.marshaller",
                ProtoStreamMarshaller.class.getName());
        clusterService.setConfigurationProperties(config.configurationProperties());

        return new RuntimeValue<>(clusterService);
    }
}
