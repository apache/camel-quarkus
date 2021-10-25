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
package org.apache.camel.quarkus.component.aws2.ses.it;

import java.util.Map;
import java.util.Optional;

import org.apache.camel.quarkus.test.support.aws2.Aws2TestEnvContext;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;

public class Aws2SesQuarkusClientTestEnvCustomizer extends Aws2SesTestEnvCustomizer {

    @Override
    public Service[] localstackServices() {
        return super.localstackServices();
    }

    @Override
    public void customize(Aws2TestEnvContext envContext) {

        super.customize(envContext);

        Map<String, String> envContextProperties = envContext.getProperies();

        envContext.property("quarkus.ses.aws.credentials.static-provider.access-key-id", envContext.getAccessKey());
        envContext.property("quarkus.ses.aws.credentials.static-provider.secret-access-key", envContext.getSecretKey());
        envContext.property("quarkus.ses.aws.region", envContext.getRegion());
        envContext.property("quarkus.ses.aws.credentials.type", "static");

        // Propagate localstack environment config to Quarkus AWS if required
        Optional<String> overrideEndpoint = envContextProperties
                .keySet()
                .stream()
                .filter(key -> key.endsWith("uri-endpoint-override"))
                .findFirst();

        if (overrideEndpoint.isPresent()) {
            String endpoint = envContextProperties.get(overrideEndpoint.get());
            envContext.property("quarkus.ses.endpoint-override", endpoint);
        }
    }
}
