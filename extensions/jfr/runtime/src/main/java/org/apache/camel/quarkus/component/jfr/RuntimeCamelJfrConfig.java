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

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.camel.jfr")
public interface RuntimeCamelJfrConfig {

    /**
     * Directory to store the recording. By default the current directory will be used. Use false to turn off saving the
     * recording to disk.
     *
     * @asciidoclet
     */
    Optional<String> startupRecorderDir();

    /**
     * How long time to run the startup recorder. Use 0 (default) to keep the recorder running until the JVM is exited. Use
     * -1 to stop the recorder right after Camel has been started (to only focus on potential Camel startup performance
     * bottlenecks) Use a positive value to keep recording for N seconds. When the recorder is stopped then the recording is
     * auto saved to disk (note: save to disk can be disabled by setting startupRecorderDir to false).
     *
     * @asciidoclet
     */
    Optional<Long> startupRecorderDuration();

    /**
     * To filter our sub steps at a maximum depth. Use -1 for no maximum. Use 0 for no sub steps. Use 1 for max 1 sub step,
     * and so forth. The default is -1.
     *
     * @asciidoclet
     */
    Optional<Integer> startupRecorderMaxDepth();

    /**
     * To use a specific Java Flight Recorder profile configuration, such as default or profile. The default is default.
     *
     * @asciidoclet
     */
    Optional<String> startupRecorderProfile();

    /**
     * To enable Java Flight Recorder to start a recording and automatic dump the recording to disk after startup is
     * complete. This requires that camel-jfr is on the classpath. The default is false.
     *
     * @asciidoclet
     */
    Optional<Boolean> startupRecorderRecording();
}
