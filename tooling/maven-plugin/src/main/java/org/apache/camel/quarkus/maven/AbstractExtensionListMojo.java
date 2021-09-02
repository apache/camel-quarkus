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
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
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
        getSkipArtifactIdBases();
        return CqUtils.findExtensions(
                getRootModuleDirectory(),
                getTree().getModulesByGa().values(),
                artifactIdBase -> !skipArtifactIdBasePatterns.matchesAny(artifactIdBase));
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
