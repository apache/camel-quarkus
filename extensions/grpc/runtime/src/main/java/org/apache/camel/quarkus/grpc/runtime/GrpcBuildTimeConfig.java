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
     *
     * @asciidoclet
     */
    @ConfigItem
    public CodeGenConfig codegen;

    @ConfigGroup
    public static class CodeGenConfig {

        /**
         * If `true`, Camel Quarkus gRPC code generation is run for .proto files discovered from the `proto` directory, or from
         * dependencies specified in the `scan-for-proto` or `scan-for-imports` options. When `false`, code generation for
         * .proto files is disabled.
         *
         * @asciidoclet
         */
        @ConfigItem(defaultValue = "true")
        public boolean enabled;

        /**
         * Camel Quarkus gRPC code generation can scan application dependencies for .proto files to generate Java stubs from
         * them. This property sets the scope of the dependencies to scan. Applicable values:
         *
         * - _none_ - default - don't scan dependencies
         * - a comma separated list of _groupId:artifactId_ coordinates to scan
         * - _all_ - scan all dependencies
         *
         * @asciidoclet
         */
        @ConfigItem(defaultValue = "none")
        public String scanForProto;

        /**
         * Camel Quarkus gRPC code generation can scan dependencies for .proto files that can be imported by protos in this
         * applications. Applicable values:
         *
         * - _none_ - default - don't scan dependencies
         * - a comma separated list of _groupId:artifactId_ coordinates to scan
         * - _all_ - scan all dependencies The default is _com.google.protobuf:protobuf-java_.
         *
         * @asciidoclet
         */
        @ConfigItem(defaultValue = "com.google.protobuf:protobuf-java")
        public String scanForImports;

        /**
         * Package path or file glob pattern includes per dependency containing .proto files to be considered for inclusion.
         *
         * @asciidoclet
         */
        @ConfigItem
        public Map<String, List<String>> scanForProtoIncludes;

        /**
         * Package path or file glob pattern includes per dependency containing .proto files to be considered for exclusion.
         *
         * @asciidoclet
         */
        @ConfigItem
        public Map<String, List<String>> scanForProtoExcludes;
    }
}
