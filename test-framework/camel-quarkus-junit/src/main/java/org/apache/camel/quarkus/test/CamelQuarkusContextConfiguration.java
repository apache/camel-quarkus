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
package org.apache.camel.quarkus.test;

import org.apache.camel.test.junit6.CamelContextConfiguration;

/**
 * Exposes protected methods from {@link CamelContextConfiguration} for use within
 * the {@code org.apache.camel.quarkus.test} package.
 */
final class CamelQuarkusContextConfiguration extends CamelContextConfiguration {

    @Override
    public CamelQuarkusContextConfiguration withCamelContextSupplier(CamelContextSupplier camelContextSupplier) {
        return (CamelQuarkusContextConfiguration) super.withCamelContextSupplier(camelContextSupplier);
    }

    @Override
    protected CamelQuarkusContextConfiguration withPostProcessor(PostProcessor postProcessor) {
        return (CamelQuarkusContextConfiguration) super.withPostProcessor(postProcessor);
    }

    @Override
    protected CamelQuarkusContextConfiguration withRoutesSupplier(RoutesSupplier routesSupplier) {
        return (CamelQuarkusContextConfiguration) super.withRoutesSupplier(routesSupplier);
    }
}
