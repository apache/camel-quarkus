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

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@ApplicationScoped
@Named("testMetric")
public class TestMetric {

    @Counted(value = "TestMetric.counted1")
    @Timed(value = "TestMetric.timed1")
    public void call1() {
        try {
            //wait 1 second
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            //do nothing
        }
    }

    @Counted(value = "TestMetric_wrong.counted2")
    public void call2() {
        //do nothing
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
}
