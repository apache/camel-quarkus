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

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import static org.apache.camel.quarkus.k.tooling.maven.support.MavenSupport.sha1Hex;

@Mojo(name = "generate-dependency-list", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true, requiresProject = false, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class GenerateDependencyListMojo extends AbstractMojo {
    private static final String[] CHECKSUM_TYPES = { "md5", "sha1" };

    @Parameter(readonly = true, defaultValue = "${project}")
    private MavenProject project;

    @Parameter(property = "dependencies.file", defaultValue = "${project.build.directory}/dependencies.yaml")
    private String outputFile;

    @Parameter(defaultValue = "true")
    private boolean includeLocation;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final Path output = Paths.get(this.outputFile);

        try {
            if (Files.notExists(output.getParent())) {
                Files.createDirectories(output.getParent());
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Exception while generating dependencies list", e);
        }

        try (Writer writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            List<Map<String, String>> deps = project.getArtifacts().stream()
                    .filter(this::isCompileOrRuntime)
                    .map(this::artifactToMap)
                    .collect(Collectors.toList());

            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

            Yaml yaml = new Yaml(options);
            yaml.dump(Collections.singletonMap("dependencies", deps), writer);
        } catch (IOException e) {
            throw new MojoExecutionException("Exception while generating dependencies list", e);
        }
    }

    private boolean isCompileOrRuntime(Artifact artifact) {
        return Objects.equals(artifact.getScope(), DefaultArtifact.SCOPE_COMPILE)
                || Objects.equals(artifact.getScope(), DefaultArtifact.SCOPE_RUNTIME);
    }

    private Map<String, String> artifactToMap(Artifact artifact) {
        Map<String, String> dep = new LinkedHashMap<>();
        dep.put("id", artifact.getId());

        if (artifact.getFile() == null) {
            return dep;
        }

        if (includeLocation) {
            dep.put("location", artifact.getFile().getAbsolutePath());
        }

        try {
            String location = artifact.getFile().getAbsolutePath();
            String checksum = null;

            for (String checksumType : CHECKSUM_TYPES) {
                Path checksumFile = Paths.get(location + "." + checksumType);
                if (Files.exists(checksumFile)) {
                    checksum = checksumType + ":" + Files.readString(checksumFile, StandardCharsets.UTF_8);
                    break;
                }
            }

            if (checksum == null) {
                try (InputStream is = Files.newInputStream(artifact.getFile().toPath())) {
                    checksum = "sha1:" + sha1Hex(is);
                }
            }

            dep.put("checksum", checksum);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        return dep;
    }
}
