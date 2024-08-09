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
package org.apache.camel.quarkus.test.support.splunk;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.commons.lang3.StringUtils;

/**
 * Test Resource meant for development. Replace port numbers with the real ports of the container running in different
 * process.
 * See README.adoc for more hints.
 */
public class FakeSplunkTestResource implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {

        String banner = StringUtils.repeat("*", 50);

        Map<String, String> m = Map.of(
                SplunkConstants.PARAM_REMOTE_HOST, "localhost",
                SplunkConstants.PARAM_TCP_PORT, "328854",
                SplunkConstants.PARAM_HEC_TOKEN, "TESTTEST-TEST-TEST-TEST-TESTTESTTEST",
                SplunkConstants.PARAM_TEST_INDEX, SplunkTestResource.TEST_INDEX,
                SplunkConstants.PARAM_REMOTE_PORT, "32885",
                SplunkConstants.PARAM_HEC_PORT, "32886");

        return m;

    }

    @Override
    public void stop() {
    }
}
