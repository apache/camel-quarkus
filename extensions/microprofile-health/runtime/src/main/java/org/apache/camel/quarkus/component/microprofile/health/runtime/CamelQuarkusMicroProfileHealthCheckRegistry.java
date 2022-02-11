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
package org.apache.camel.quarkus.component.microprofile.health.runtime;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;

import io.smallrye.health.api.HealthRegistry;
import io.smallrye.health.registry.LivenessHealthRegistry;
import io.smallrye.health.registry.ReadinessHealthRegistry;
import org.apache.camel.CamelContext;
import org.apache.camel.microprofile.health.CamelMicroProfileHealthCheckRegistry;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;

public class CamelQuarkusMicroProfileHealthCheckRegistry extends CamelMicroProfileHealthCheckRegistry {

    CamelQuarkusMicroProfileHealthCheckRegistry(CamelContext camelContext) {
        super(camelContext);
    }

    @Override
    protected HealthRegistry getLivenessRegistry() {
        return getHealthRegistryBean(LivenessHealthRegistry.class, Liveness.Literal.INSTANCE);
    }

    @Override
    protected HealthRegistry getReadinessRegistry() {
        return getHealthRegistryBean(ReadinessHealthRegistry.class, Readiness.Literal.INSTANCE);
    }

    private static HealthRegistry getHealthRegistryBean(Class<? extends HealthRegistry> type, Annotation qualifier) {
        BeanManager beanManager = CDI.current().getBeanManager();
        Set<Bean<?>> beans = beanManager.getBeans(type, qualifier);
        if (beans.isEmpty()) {
            throw new IllegalStateException(
                    "Beans for type " + type.getName() + " with qualifier " + qualifier + " could not be found.");
        }

        Bean<?> bean = beanManager.resolve(beans);
        Object reference = beanManager.getReference(bean, type, beanManager.createCreationalContext(bean));
        return type.cast(reference);
    }
}
