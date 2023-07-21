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
package org.apache.camel.quarkus.component.micrometer.it;

import java.util.Arrays;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;
import io.micrometer.jmx.JmxMeterRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.micrometer.runtime.MeterFilterConstraint;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.camel.component.micrometer.CamelJmxConfig;
import org.apache.camel.component.micrometer.MicrometerComponent;

public class MicrometerProducers {

    @Produces
    @Singleton
    @IfBuildProfile("test")
    public MeterRegistry registry(Clock clock) {
        return new JmxMeterRegistry(CamelJmxConfig.DEFAULT, Clock.SYSTEM, HierarchicalNameMapper.DEFAULT);
    }

    @Produces
    @Singleton
    @MeterFilterConstraint(applyTo = PrometheusMeterRegistry.class)
    public MeterFilter configurePrometheusRegistries() {
        return MeterFilter.commonTags(Arrays.asList(
                Tag.of("customTag", "prometheus")));
    }

    @Produces
    @Singleton
    public MeterFilter renameApplicationMeters() {
        final String targetMetric = "TestMetric_wrong.counted2";

        return new MeterFilter() {
            @Override
            public Meter.Id map(Meter.Id id) {
                if (id.getName().equals(targetMetric)) {
                    // rename the metric
                    return id.withName("TestMetric.counted2");
                }
                return id;
            }
        };
    }

    @Singleton
    @Produces
    @Named("micrometerCustom")
    MicrometerComponent micrometerCustomComponent() {
        MicrometerComponent component = new MicrometerComponent();

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        component.setMetricsRegistry(registry);

        return component;
    }

}
