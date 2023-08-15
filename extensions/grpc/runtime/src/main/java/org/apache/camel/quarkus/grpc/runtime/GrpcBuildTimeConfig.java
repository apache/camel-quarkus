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
package org.apache.camel.quarkus.grpc.runtime;

import java.util.List;
import java.util.Map;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "camel.grpc", phase = ConfigPhase.BUILD_TIME)
public class GrpcBuildTimeConfig {
    /**
     * Build time configuration options for Camel Quarkus gRPC code generator.
     */
    @ConfigItem
    public CodeGenConfig codegen;

    @ConfigGroup
    public static class CodeGenConfig {
        /**
         * If {@code true}, Camel Quarkus gRPC code generation is run for .proto files discovered from the {@code proto}
         * directory, or from dependencies specified in the {@code scan-for-proto} or {@code scan-for-imports} options. When
         * {@code false}, code generation for .proto files is disabled.
         */
        @ConfigItem(defaultValue = "true")
        public boolean enabled;

        /**
         * Camel Quarkus gRPC code generation can scan application dependencies for .proto files to generate Java stubs
         * from them.
         * This property sets the scope of the dependencies to scan.
         * Applicable values:
         * <ul>
         * <li><i>none</i> - default - don't scan dependencies</li>
         * <li>a comma separated list of <i>groupId:artifactId</i> coordinates to scan</li>
         * <li><i>all</i> - scan all dependencies</li>
         * </ul>
         */
        @ConfigItem(defaultValue = "none")
        public String scanForProto;

        /**
         * Camel Quarkus gRPC code generation can scan dependencies for .proto files that can be imported by protos in this
         * applications.
         * Applicable values:
         * <ul>
         * <li><i>none</i> - default - don't scan dependencies</li>
         * <li>a comma separated list of <i>groupId:artifactId</i> coordinates to scan</li>
         * <li><i>all</i> - scan all dependencies</li>
         * </ul>
         *
         * The default is <i>com.google.protobuf:protobuf-java</i>.
         */
        @ConfigItem(defaultValue = "com.google.protobuf:protobuf-java")
        public String scanForImports;

        /**
         * Package path or file glob pattern includes per dependency containing .proto files to be considered for inclusion.
         */
        @ConfigItem
        public Map<String, List<String>> scanForProtoIncludes;

        /**
         * Package path or file glob pattern includes per dependency containing .proto files to be considered for exclusion.
         */
        @ConfigItem
        public Map<String, List<String>> scanForProtoExcludes;
    }
}
