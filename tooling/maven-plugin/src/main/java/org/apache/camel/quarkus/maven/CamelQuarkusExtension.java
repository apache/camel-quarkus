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

import org.apache.camel.catalog.Kind;
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
    public static final String CAMEL_QUARKUS_KIND = "camel.quarkus.kind";

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
            final boolean nativeSupported = !runtimePomXmlPath.getParent().getParent().getParent().getFileName().toString()
                    .endsWith("-jvm");
            final String extensionStatus = props.getProperty("quarkus.metadata.status");
            final ExtensionStatus status = extensionStatus == null ? ExtensionStatus.of(nativeSupported)
                    : ExtensionStatus.valueOf(extensionStatus);
            final boolean unlisted = !nativeSupported
                    || Boolean.parseBoolean(props.getProperty("quarkus.metadata.unlisted", "false"));
            final boolean deprecated = Boolean.parseBoolean(props.getProperty("quarkus.metadata.deprecated", "false"));

            final String rawKind = (String) props.get(CAMEL_QUARKUS_KIND);
            final Kind kind = rawKind == null ? null : Kind.valueOf(rawKind);

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
                    nativeSupported,
                    status,
                    unlisted,
                    deprecated,
                    deps == null ? Collections.emptyList() : Collections.unmodifiableList(deps),
                    kind,
                    props.getProperty("cq.quarkus.aws.client.baseName"),
                    props.getProperty("cq.quarkus.aws.client.fqClassName"));
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
    private final ExtensionStatus status;
    private final boolean unlisted;
    private final boolean deprecated;
    private final Kind kind;
    private final String quarkusAwsClientBaseName;
    private final String quarkusAwsClientFqClassName;

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
            ExtensionStatus status,
            boolean unlisted,
            boolean deprecated,
            List<Dependency> dependencies,
            Kind kind,
            String quarkusAwsClientBaseName,
            String quarkusAwsClientFqClassName) {
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
        this.status = status;
        this.unlisted = unlisted;
        this.deprecated = deprecated;
        this.dependencies = dependencies;
        this.kind = kind;
        this.quarkusAwsClientBaseName = quarkusAwsClientBaseName;
        this.quarkusAwsClientFqClassName = quarkusAwsClientFqClassName;
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

    public ExtensionStatus getStatus() {
        return status;
    }

    public boolean isUnlisted() {
        return unlisted;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public Kind getKind() {
        return kind;
    }

    public Optional<String> getQuarkusAwsClientBaseName() {
        return Optional.ofNullable(quarkusAwsClientBaseName);
    }

    public Optional<String> getQuarkusAwsClientFqClassName() {
        return Optional.ofNullable(quarkusAwsClientFqClassName);
    }

}
