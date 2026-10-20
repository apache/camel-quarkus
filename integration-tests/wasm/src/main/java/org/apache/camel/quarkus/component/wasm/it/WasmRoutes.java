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
package org.apache.camel.quarkus.component.wasm.it;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.camel.builder.RouteBuilder;

public class WasmRoutes extends RouteBuilder {
    static final Path WASM_MODULE_PATH = Paths.get("target/wasm/modules");
    static final Path WASM_UPPER = WASM_MODULE_PATH.resolve("upper.wasm");

    @Override
    public void configure() throws Exception {
        copyUpperToFilesystem();

        from("direct:executeFunctionFromClasspath")
                .to("wasm:wasm/modules/upper.wasm");

        from("direct:executeFunctionFromFile")
                .to("wasm:file:target/wasm/modules/upper.wasm");

        from("direct:executeFunctionError")
                .to("wasm:wasm/modules/error.wasm");
    }

    static void copyUpperToFilesystem() throws IOException {
        Files.createDirectories(WASM_MODULE_PATH);
        try (InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("wasm/modules/upper.wasm")) {
            if (stream == null) {
                throw new RuntimeException("Failed to read upper.wasm from the classpath");
            }
            Files.copy(stream, WASM_UPPER);
        }
    }
}
