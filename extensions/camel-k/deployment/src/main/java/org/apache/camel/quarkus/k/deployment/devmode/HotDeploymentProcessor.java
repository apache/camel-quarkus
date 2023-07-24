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
package org.apache.camel.quarkus.k.deployment.devmode;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import org.apache.camel.quarkus.k.support.Constants;
import org.apache.camel.util.StringHelper;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HotDeploymentProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HotDeploymentProcessor.class);

    @BuildStep
    List<HotDeploymentWatchedFileBuildItem> routes() {
        final Config config = ConfigProvider.getConfig();
        final Optional<String> value = config.getOptionalValue(Constants.PROPERTY_CAMEL_K_ROUTES, String.class);

        List<HotDeploymentWatchedFileBuildItem> items = new ArrayList<>();

        if (value.isPresent()) {
            for (String source : value.get().split(",", -1)) {
                String path = StringHelper.after(source, ":");
                if (path == null) {
                    path = source;
                }

                Path p = Paths.get(path);
                if (Files.exists(p)) {
                    LOGGER.info("Register source for hot deployment: {}", p.toAbsolutePath());
                    items.add(new HotDeploymentWatchedFileBuildItem(p.toAbsolutePath().toString()));
                }
            }
        }

        return items;
    }

    @BuildStep
    List<HotDeploymentWatchedFileBuildItem> conf() {
        final Config config = ConfigProvider.getConfig();
        final Optional<String> conf = config.getOptionalValue(Constants.PROPERTY_CAMEL_K_CONF, String.class);
        final Optional<String> confd = config.getOptionalValue(Constants.PROPERTY_CAMEL_K_CONF_D, String.class);

        List<HotDeploymentWatchedFileBuildItem> items = new ArrayList<>();

        if (conf.isPresent()) {
            LOGGER.info("Register conf for hot deployment: {}", conf.get());
            items.add(new HotDeploymentWatchedFileBuildItem(conf.get()));
        }

        if (confd.isPresent()) {
            FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Objects.requireNonNull(file);
                    Objects.requireNonNull(attrs);

                    String path = file.toFile().getAbsolutePath();
                    String ext = FilenameUtils.getExtension(path);

                    if (Objects.equals("properties", ext)) {
                        LOGGER.info("Register conf for hot deployment: {}", path);
                        items.add(new HotDeploymentWatchedFileBuildItem(path));
                    }

                    return FileVisitResult.CONTINUE;
                }
            };

            Path root = Paths.get(confd.get());
            if (Files.exists(root)) {
                try {
                    Files.walkFileTree(root, visitor);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return items;
    }
}
