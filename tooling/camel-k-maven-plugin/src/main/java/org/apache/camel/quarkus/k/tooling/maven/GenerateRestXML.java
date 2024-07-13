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
package org.apache.camel.quarkus.k.tooling.maven;

import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.apache.camel.CamelContext;
import org.apache.camel.generator.openapi.RestDslXmlGenerator;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "generate-rest-xml", inheritByDefault = false, defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresProject = false, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
class GenerateRestXML extends AbstractMojo {
    @Parameter(property = "openapi.spec")
    private String inputFile;
    @Parameter(property = "dsl.out")
    private String outputFile;

    @Override
    public void execute() throws MojoExecutionException {
        if (inputFile == null) {
            throw new MojoExecutionException("Missing input file: " + inputFile);
        }

        Path input = Paths.get(this.inputFile);
        if (!Files.exists(input)) {
            throw new MojoExecutionException("Unable to read the input file: " + inputFile);
        }

        try {
            JsonFactory factory = null;
            if (inputFile.endsWith(".yaml") || inputFile.endsWith(".yml")) {
                factory = new YAMLFactory();
            }

            ObjectMapper mapper = new ObjectMapper(factory);
            mapper.findAndRegisterModules();

            OpenAPIV3Parser parser = new OpenAPIV3Parser();
            OpenAPI document = parser.read("file:" + input.toAbsolutePath());

            final Writer writer;

            if (outputFile != null) {
                Path output = Paths.get(this.outputFile);

                if (output.getParent() != null && Files.notExists(output.getParent())) {
                    Files.createDirectories(output.getParent());
                }
                if (Files.exists(output)) {
                    Files.delete(output);
                }

                writer = Files.newBufferedWriter(output);
            } else {
                writer = new PrintWriter(System.out);
            }

            final CamelContext context = new DefaultCamelContext();
            final String dsl = RestDslXmlGenerator.toXml(document).generate(context);

            try (writer) {
                writer.write(dsl);
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Exception while generating rest xml", e);
        }
    }
}
