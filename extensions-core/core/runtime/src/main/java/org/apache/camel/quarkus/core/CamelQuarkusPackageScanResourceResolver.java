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
package org.apache.camel.quarkus.core;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

import io.quarkus.runtime.ImageMode;
import org.apache.camel.impl.engine.DefaultPackageScanResourceResolver;
import org.apache.camel.spi.PackageScanResourceResolver;
import org.apache.camel.spi.Resource;
import org.apache.camel.spi.ResourceLoader;
import org.apache.camel.support.PluginHelper;
import org.apache.camel.support.ResourceHelper;
import org.apache.camel.util.AntPathMatcher;
import org.apache.camel.util.ObjectHelper;
import org.jboss.logging.Logger;

/**
 * Custom {@link PackageScanResourceResolver} that adds the specific {@link ClassLoader} instance
 * that Camel Quarkus requires for resolving resources. Also performs native image capable classpath glob pattern
 * resource resolution.
 */
public class CamelQuarkusPackageScanResourceResolver extends DefaultPackageScanResourceResolver {
    private static final Logger LOG = Logger.getLogger(CamelQuarkusPackageScanResourceResolver.class);
    private static final URI NATIVE_IMAGE_FILESYSTEM_URI = URI.create("resource:/");
    private FileSystem resourceFileSystem;

    @Override
    public void initialize() {
        addClassLoader(Thread.currentThread().getContextClassLoader());
    }

    @Override
    protected void doStop() throws Exception {
        if (resourceFileSystem != null) {
            resourceFileSystem.close();
            resourceFileSystem = null;
        }
    }

    @Override
    public Collection<Resource> findResources(String location) throws Exception {
        Collection<Resource> resources = super.findResources(location);

        // If no matches were found for the location pattern in native mode, try to use the resource scheme filesystem
        if (resources.isEmpty() && ImageMode.current().isNativeImage()) {
            String scheme = ResourceHelper.getScheme(location);
            if (isClassPathPattern(location, scheme)) {
                FileSystem fileSystem = getNativeImageResourceFileSystem();
                ResourceLoader resourceLoader = PluginHelper.getResourceLoader(getCamelContext());
                String root = AntPathMatcher.INSTANCE.determineRootDir(location);
                String rootWithoutScheme = scheme != null ? root.substring(scheme.length()) : root;
                String subPattern = location.substring(root.length());
                Path startPath = fileSystem.getPath(rootWithoutScheme);

                if (!Files.exists(startPath)) {
                    LOG.tracef("Failed to find resources for location: %s as path %s does not exist", location, startPath);
                    return resources;
                }

                LOG.tracef("Finding native resources for location: %s, sub pattern %s, under path %s", location, subPattern,
                        rootWithoutScheme);

                // Iterate classpath resources in the native application and try to find matches
                Files.walkFileTree(startPath, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        Path pathToMatch = file.getNameCount() > 1 ? file.subpath(1, file.getNameCount()) : file.getFileName();

                        LOG.tracef("Checking for native resource match for: %s", pathToMatch);
                        if (AntPathMatcher.INSTANCE.match(subPattern, pathToMatch.toString())) {
                            LOG.tracef("Matched native resource: %s with pattern: %s", file, subPattern);
                            resources.add(resourceLoader.resolveResource(file.toString()));
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        LOG.tracef(exc, "Failed to process resource: %s", file);
                        return FileVisitResult.CONTINUE;
                    }
                });

                // Fallback on matching files explicitly in the resolved directory path
                if (resources.isEmpty() && Files.isDirectory(startPath)) {
                    try (Stream<Path> files = Files.list(startPath)) {
                        files.map(Path::getFileName)
                                .map(Path::toString)
                                .filter(path -> AntPathMatcher.INSTANCE.match(subPattern, path))
                                .map(path -> startPath.resolve(path).toString())
                                .peek(path -> LOG.tracef("Matched native resource: %s with pattern: %s", path, subPattern))
                                .map(resourceLoader::resolveResource)
                                .forEach(resources::add);
                    }
                }
            }
        }

        return resources;
    }

    protected boolean isClassPathPattern(String location, String scheme) {
        if (ObjectHelper.isNotEmpty(location)) {
            return (AntPathMatcher.INSTANCE.isPattern(location))
                    && (ObjectHelper.isEmpty(scheme) || "classpath:".equals(scheme));
        }
        return false;
    }

    protected FileSystem getNativeImageResourceFileSystem() {
        // Must lazy init the FileSystem at runtime so that resources are discoverable
        if (resourceFileSystem == null) {
            try {
                resourceFileSystem = FileSystems.newFileSystem(NATIVE_IMAGE_FILESYSTEM_URI,
                        Collections.singletonMap("create", "true"));
            } catch (FileSystemAlreadyExistsException ex) {
                resourceFileSystem = FileSystems.getFileSystem(NATIVE_IMAGE_FILESYSTEM_URI);
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }
        return resourceFileSystem;
    }
}
