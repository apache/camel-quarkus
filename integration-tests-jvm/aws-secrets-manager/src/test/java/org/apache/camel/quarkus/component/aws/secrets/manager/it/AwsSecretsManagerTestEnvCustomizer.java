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
package org.apache.camel.quarkus.component.aws.secrets.manager.it;

import java.util.Map;
import java.util.stream.Collectors;

import org.apache.camel.quarkus.test.support.aws2.Aws2TestEnvContext;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestEnvCustomizer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;

public class AwsSecretsManagerTestEnvCustomizer implements Aws2TestEnvCustomizer {

    @Override
    public Service[] localstackServices() {
        return new Service[] { Service.SECRETSMANAGER };
    }

    @Override
    public void customize(Aws2TestEnvContext envContext) {

        //get all properties from the context
        Map<String, String> props = envContext.getProperties();
        //gather all properties (witch changed aws2 -> aws)
        Map<String, String> p2 = props.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().replaceFirst("aws2", "aws"), e -> e.getValue()));
        //remove client (to get rid of properties containing aws2)
        envContext.removeClient(new Service[] { Service.SECRETSMANAGER });
        envContext.removeOverrideEndpoint(new Service[] { Service.SECRETSMANAGER });

        for (Map.Entry<String, String> e : p2.entrySet()) {
            envContext.property(e.getKey(), e.getValue());
        }
    }
}
