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
package org.apache.camel.quarkus.maven;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * A utility to extract some extension metadata from the Runtime module's POM.
 */
public class CamelQuarkusExtension {

    public static final String CAMEL_QUARKUS_JVM_SINCE = "camel.quarkus.jvmSince";
    public static final String CAMEL_QUARKUS_NATIVE_SINCE = "camel.quarkus.nativeSince";

    public static CamelQuarkusExtension read(Path runtimePomXmlPath) {
        try (Reader runtimeReader = Files.newBufferedReader(runtimePomXmlPath, StandardCharsets.UTF_8)) {
            final MavenXpp3Reader rxppReader = new MavenXpp3Reader();
            final Model runtimePom = rxppReader.read(runtimeReader);
            final List<Dependency> deps = runtimePom.getDependencies();

            final String aid = runtimePom.getArtifactId();
            String camelComponentArtifactId = null;
            if (deps != null && !deps.isEmpty()) {
                Optional<Dependency> artifact = deps.stream()
                        .filter(dep ->

                        "org.apache.camel".equals(dep.getGroupId()) &&
                                ("compile".equals(dep.getScope()) || dep.getScope() == null))
                        .findFirst();
                if (artifact.isPresent()) {
                    camelComponentArtifactId = CqCatalog.toCamelComponentArtifactIdBase(artifact.get().getArtifactId());
                }
            }
            final Properties props = runtimePom.getProperties() != null ? runtimePom.getProperties() : new Properties();

            String name = props.getProperty("title");
            if (name == null) {
                name = CqUtils.getNameBase(runtimePom.getName());
            }

            final String version = CqUtils.getVersion(runtimePom);

            return new CamelQuarkusExtension(
                    runtimePomXmlPath,
                    camelComponentArtifactId,
                    (String) props.get(CAMEL_QUARKUS_JVM_SINCE),
                    (String) props.get(CAMEL_QUARKUS_NATIVE_SINCE),
                    aid,
                    name,
                    runtimePom.getDescription(),
                    props.getProperty("label"),
                    version,
                    !runtimePomXmlPath.getParent().getParent().getParent().getFileName().toString().endsWith("-jvm"),
                    deps == null ? Collections.emptyList() : Collections.unmodifiableList(deps));
        } catch (IOException | XmlPullParserException e) {
            throw new RuntimeException("Could not read " + runtimePomXmlPath, e);
        }
    }

    private final String label;
    private final String version;
    private final String description;
    private final String runtimeArtifactId;
    private final Path runtimePomXmlPath;
    private final String camelComponentArtifactId;
    private final String jvmSince;
    private final String name;
    private final boolean nativeSupported;
    private final String nativeSince;
    private final List<Dependency> dependencies;

    public CamelQuarkusExtension(
            Path runtimePomXmlPath,
            String camelComponentArtifactId,
            String jvmSince,
            String nativeSince,
            String runtimeArtifactId,
            String name,
            String description,
            String label,
            String version,
            boolean nativeSupported,
            List<Dependency> dependencies) {
        super();
        this.runtimePomXmlPath = runtimePomXmlPath;
        this.camelComponentArtifactId = camelComponentArtifactId;
        this.jvmSince = jvmSince;
        this.nativeSince = nativeSince;
        this.runtimeArtifactId = runtimeArtifactId;
        this.name = name;
        this.description = description;
        this.label = label;
        this.version = version;
        this.nativeSupported = nativeSupported;
        this.dependencies = dependencies;
    }

    public String getVersion() {
        return version;
    }

    public Optional<String> getJvmSince() {
        return Optional.ofNullable(jvmSince);
    }

    public Path getRuntimePomXmlPath() {
        return runtimePomXmlPath;
    }

    public Optional<String> getLabel() {
        return Optional.ofNullable(label);
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public String getRuntimeArtifactIdBase() {
        return CqUtils.getArtifactIdBase(runtimeArtifactId);
    }

    public String getRuntimeArtifactId() {
        return runtimeArtifactId;
    }

    public String getCamelComponentArtifactId() {
        return camelComponentArtifactId;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public boolean isNativeSupported() {
        return nativeSupported;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public Optional<String> getNativeSince() {
        return Optional.ofNullable(nativeSince);
    }

}
