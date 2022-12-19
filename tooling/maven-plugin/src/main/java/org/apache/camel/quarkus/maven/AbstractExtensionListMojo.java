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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.l2x6.maven.utils.MavenSourceTree;

public abstract class AbstractExtensionListMojo extends AbstractMojo {

    /**
     * A set of regular expressions to match against artifactIdBases. The matching extensions will not be processed by
     * this mojo.
     *
     * @since 0.1.0
     */
    @Parameter(property = "cq.skipArtifactIdBases")
    protected List<String> skipArtifactIdBases;
    private PatternSet skipArtifactIdBasePatterns;

    /**
     * The root directory of the Camel Quarkus source tree.
     *
     * @since 0.41.0
     */
    @Parameter(defaultValue = "${maven.multiModuleProjectDirectory}", readonly = true)
    protected File multiModuleProjectDirectory;
    private Path rootModuleDirectory;

    /**
     * Encoding to read and write files in the current source tree
     *
     * @since 0.18.0
     */
    @Parameter(defaultValue = "utf-8", required = true, property = "cq.encoding")
    String encoding;
    private Charset charset;

    private MavenSourceTree tree;

    Path getRootModuleDirectory() {
        if (rootModuleDirectory == null) {
            rootModuleDirectory = multiModuleProjectDirectory.toPath().toAbsolutePath().normalize();
        }
        return rootModuleDirectory;
    }

    Charset getCharset() {
        if (charset == null) {
            charset = Charset.forName(encoding);
        }
        return charset;
    }

    Stream<ExtensionModule> findExtensions() {
        return findExtensions(true);
    }

    /**
     * @param  strict if {@code true} only Maven modules are considered that are reachable over a {@code <module>}
     *                element from the root module; otherwise also unreachable modules will be returned.
     * @return
     */
    Stream<ExtensionModule> findExtensions(boolean strict) {
        getSkipArtifactIdBases();
        final Stream<ExtensionModule> strictModulesStream = CqUtils.findExtensions(
                getRootModuleDirectory(),
                getTree().getModulesByGa().values(),
                artifactIdBase -> !skipArtifactIdBasePatterns.matchesAny(artifactIdBase));
        if (strict) {
            return strictModulesStream;
        } else {
            final Collection<ExtensionModule> strictModules = strictModulesStream
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            final Set<ExtensionModule> result = new TreeSet<>();
            strictModules.stream()
                    .map(module -> module.getExtensionDir().getParent())
                    .distinct()
                    .forEach(extensionDir -> {

                        try (Stream<Path> files = Files.list(extensionDir)) {
                            files
                                    .filter(Files::isDirectory)
                                    .map(extensionDirectory -> extensionDirectory.resolve("runtime/pom.xml"))
                                    .filter(Files::isRegularFile)
                                    .forEach(runtimePomXmlPath -> {
                                        String artifactId = null;
                                        try (Reader r = Files.newBufferedReader(runtimePomXmlPath, StandardCharsets.UTF_8)) {
                                            final MavenXpp3Reader rxppReader = new MavenXpp3Reader();
                                            final Model model = rxppReader.read(r);
                                            artifactId = model.getArtifactId();
                                        } catch (IOException | XmlPullParserException e) {
                                            throw new RuntimeException("Could not read " + runtimePomXmlPath);
                                        }
                                        if (!artifactId.startsWith("camel-quarkus-")) {
                                            throw new IllegalStateException(
                                                    "Should start with 'camel-quarkus-': " + artifactId);
                                        }
                                        final String artifactIdBase = artifactId.substring("camel-quarkus-".length());
                                        if (!skipArtifactIdBasePatterns.matchesAny(artifactIdBase)) {
                                            result.add(new ExtensionModule(runtimePomXmlPath.getParent().getParent(),
                                                    artifactIdBase));
                                        }
                                    });
                        } catch (IOException e) {
                            throw new RuntimeException("Could not list " + extensionDir, e);
                        }
                    });
            return result.stream();
        }
    }

    PatternSet getSkipArtifactIdBases() {
        if (skipArtifactIdBasePatterns == null) {
            skipArtifactIdBasePatterns = skipArtifactIdBases == null ? PatternSet.empty() : new PatternSet(skipArtifactIdBases);
        }
        return skipArtifactIdBasePatterns;
    }

    public MavenSourceTree getTree() {
        if (tree == null) {
            tree = MavenSourceTree.of(getRootModuleDirectory().resolve("pom.xml"), getCharset());
        }
        return tree;
    }

}
