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
package org.apache.camel.quarkus.component.jfr;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.CamelContext;
import org.apache.camel.spi.CamelContextCustomizer;
import org.apache.camel.startup.jfr.FlightRecorderStartupStepRecorder;

@Recorder
public class CamelJfrRecorder {
    private final RuntimeValue<RuntimeCamelJfrConfig> runtimeConfig;

    public CamelJfrRecorder(RuntimeValue<RuntimeCamelJfrConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public RuntimeValue<CamelContextCustomizer> createStartupStepRecorder() {
        CamelContextCustomizer flightRecorderCustomizer = new CamelContextCustomizer() {
            @Override
            public void configure(CamelContext camelContext) {
                FlightRecorderStartupStepRecorder flightRecorder = new FlightRecorderStartupStepRecorder();

                if (runtimeConfig.getValue().startupRecorderRecording().isPresent()) {
                    flightRecorder.setRecording(runtimeConfig.getValue().startupRecorderRecording().get());
                }

                if (runtimeConfig.getValue().startupRecorderProfile().isPresent()) {
                    flightRecorder.setRecordingProfile(runtimeConfig.getValue().startupRecorderProfile().get());
                }

                if (runtimeConfig.getValue().startupRecorderMaxDepth().isPresent()) {
                    flightRecorder.setMaxDepth(runtimeConfig.getValue().startupRecorderMaxDepth().get());
                }

                if (runtimeConfig.getValue().startupRecorderDuration().isPresent()) {
                    flightRecorder.setStartupRecorderDuration(runtimeConfig.getValue().startupRecorderDuration().get());
                }

                if (runtimeConfig.getValue().startupRecorderDir().isPresent()) {
                    flightRecorder.setRecordingDir(runtimeConfig.getValue().startupRecorderDir().get());
                }

                camelContext.getCamelContextExtension().setStartupStepRecorder(flightRecorder);
                flightRecorder.setEnabled(true);
                flightRecorder.start();
            }
        };
        return new RuntimeValue<>(flightRecorderCustomizer);
    }
}
