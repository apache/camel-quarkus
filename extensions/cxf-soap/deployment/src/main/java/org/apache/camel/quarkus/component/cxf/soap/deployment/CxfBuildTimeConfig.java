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

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "camel.cxf", phase = ConfigPhase.BUILD_TIME)
public class CxfBuildTimeConfig {

    /** Configuration options related to build time class generation */
    @ConfigItem
    public ClassGeneration classGeneration;

    @ConfigGroup
    public static class ClassGeneration {
        /**
         * For CXF service interfaces to work properly, some ancillary classes (such as request and response
         * wrappers) need to be generated at build time. Camel Quarkus lets the {@code quarkus-cxf} extension to do this
         * for all service interfaces found in the class path except the ones matching the patterns in this property.
         * <p>
         * {@code org.apache.cxf.ws.security.sts.provider.SecurityTokenService} is excluded by default due to
         * <a href="https://issues.apache.org/jira/browse/CXF-8834">https://issues.apache.org/jira/browse/CXF-8834</a>
         */
        @ConfigItem(defaultValue = "org.apache.cxf.ws.security.sts.provider.SecurityTokenService")
        List<String> excludePatterns;

    }

}
