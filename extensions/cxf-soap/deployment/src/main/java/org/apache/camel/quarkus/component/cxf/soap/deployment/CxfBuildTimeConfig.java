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
package org.apache.camel.quarkus.component.cxf.soap.deployment;

import java.util.List;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "quarkus.camel.cxf")
public interface CxfBuildTimeConfig {
    /**
     * Configuration options related to build time class generation
     *
     * @asciidoclet
     */
    ClassGeneration classGeneration();

    interface ClassGeneration {
        /**
         * For CXF service interfaces to work properly, some ancillary classes (such as request and response wrappers) need to
         * be generated at build time. Camel Quarkus lets the `quarkus-cxf` extension to do this for all service interfaces
         * found in the class path except the ones matching the patterns in this property.
         *
         * `org.apache.cxf.ws.security.sts.provider.SecurityTokenService` is excluded by default due to
         * link:https://issues.apache.org/jira/browse/CXF-8834[https://issues.apache.org/jira/browse/CXF-8834]
         *
         * @asciidoclet
         */
        @WithDefault("org.apache.cxf.ws.security.sts.provider.SecurityTokenService")
        List<String> excludePatterns();
    }
}
