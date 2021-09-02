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

import java.nio.file.Path;

public class ExtensionModule implements Comparable<ExtensionModule> {

    private final Path extensionDir;
    private final String artifactIdBase;

    public ExtensionModule(Path extensionDir, String artifactIdBase) {
        super();
        this.extensionDir = extensionDir;
        this.artifactIdBase = artifactIdBase;
    }

    /**
     * @return the absolute canonical path of the directory whose children are the runtime and deployment directories of
     *         this {@link ExtensionModule}
     */
    public Path getExtensionDir() {
        return extensionDir;
    }

    public String getArtifactIdBase() {
        return artifactIdBase;
    }

    @Override
    public String toString() {
        return artifactIdBase + " (" + extensionDir + ")";
    }

    public boolean isNativeSupported() {
        return !extensionDir.getParent().getFileName().toString().equals("extensions-jvm");
    }

    @Override
    public int compareTo(ExtensionModule other) {
        return this.artifactIdBase.compareTo(other.artifactIdBase);
    }

    public Path getRuntimePomPath() {
        return extensionDir.resolve("runtime/pom.xml");
    }

    public Path getPomPath() {
        return extensionDir.resolve("pom.xml");
    }

}
