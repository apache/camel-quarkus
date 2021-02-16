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
package org.apache.camel.quarkus.test.support.aws2;

import org.testcontainers.containers.localstack.LocalStackContainer.Service;

/**
 * An SPI to allow individual AWS 2 test modules to customize the {@link Aws2TestResource}.
 * At the same time, this SPI should allow running the AWS 2 test modules both isolated and merged together.
 */
public interface Aws2TestEnvCustomizer {

    /**
     * @return an array of services the Localstack container should expose
     */
    Service[] localstackServices();

    /**
     * @return an array of Localstack services for which {@link Aws2TestEnvContext} should export credentials properties
     */
    default Service[] exportCredentialsForLocalstackServices() {
        return localstackServices();
    }

    /**
     * Customize the given {@link Aws2TestEnvContext}
     *
     * @param envContext the {@link Aws2TestEnvContext} to customize
     */
    void customize(Aws2TestEnvContext envContext);
}
