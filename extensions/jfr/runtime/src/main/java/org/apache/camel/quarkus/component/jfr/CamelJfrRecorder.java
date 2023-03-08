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

    public RuntimeValue<CamelContextCustomizer> createStartupStepRecorder(RuntimeCamelJfrConfig config) {
        CamelContextCustomizer flightRecorderCustomizer = new CamelContextCustomizer() {
            @Override
            public void configure(CamelContext camelContext) {
                FlightRecorderStartupStepRecorder flightRecorder = new FlightRecorderStartupStepRecorder();

                if (config.startupRecorderRecording.isPresent()) {
                    flightRecorder.setRecording(config.startupRecorderRecording.get());
                }

                if (config.startupRecorderProfile.isPresent()) {
                    flightRecorder.setRecordingProfile(config.startupRecorderProfile.get());
                }

                if (config.startupRecorderMaxDepth.isPresent()) {
                    flightRecorder.setMaxDepth(config.startupRecorderMaxDepth.get());
                }

                if (config.startupRecorderDuration.isPresent()) {
                    flightRecorder.setStartupRecorderDuration(config.startupRecorderDuration.get());
                }

                if (config.startupRecorderDir.isPresent()) {
                    flightRecorder.setRecordingDir(config.startupRecorderDir.get());
                }

                camelContext.getCamelContextExtension().setStartupStepRecorder(flightRecorder);
                flightRecorder.setEnabled(true);
                flightRecorder.start();
            }
        };
        return new RuntimeValue<>(flightRecorderCustomizer);
    }
}
