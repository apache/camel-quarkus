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
package org.apache.camel.quarkus.component.log.it;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class MdcLoggingTestProfile implements QuarkusTestProfile {
    public static final String CONTEXT_NAME = "mdc-logging";

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.log.file.path", "target/quarkus.log",
                "quarkus.log.file.format", "MDC[%X,%m]%n",
                "camel.main.javaRoutesExcludePattern", "**/LogRoutes",
                "camel.main.routeFilterIncludePattern", "direct:mdcLog*",
                "camel.context.name", CONTEXT_NAME,
                "camel.main.useMdcLogging", "true",
                "camel.main.useBreadcrumb", "true");
    }
}
