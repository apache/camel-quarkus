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
package org.apache.camel.quarkus.main.deployment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * camel-main does not yet support filtering with pattern/glob so
 * each entry of camel.main.xml-[routes|rests] is a path thus can
 * be safely added to the list of files to watch to trigger hot
 * deployment.
 *
 * See https://issues.apache.org/jira/browse/CAMEL-14100
 */
class CamelMainHotDeploymentProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CamelMainHotDeploymentProcessor.class);
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final String FILE_PREFIX = "file:";

    @BuildStep
    List<HotDeploymentWatchedFileBuildItem> xmlRoutes() {
        return locations("camel.main.xml-routes");
    }

    @BuildStep
    List<HotDeploymentWatchedFileBuildItem> xmlRests() {
        return locations("camel.main.xml-rests");
    }

    private static List<HotDeploymentWatchedFileBuildItem> locations(String property) {
        String[] locations = ConfigProvider.getConfig()
                .getOptionalValue(property, String[].class)
                .orElse(EMPTY_STRING_ARRAY);

        List<HotDeploymentWatchedFileBuildItem> items = Stream.of(locations)
                .filter(location -> location.startsWith(FILE_PREFIX))
                .map(location -> location.substring(FILE_PREFIX.length()))
                .distinct()
                .map(Paths::get)
                .filter(Files::exists)
                .map(Path::toAbsolutePath)
                .map(Path::toString)
                .map(HotDeploymentWatchedFileBuildItem::new)
                .collect(Collectors.toList());

        if (!items.isEmpty()) {
            LOGGER.info("HotDeployment files ({}):", property);
            for (HotDeploymentWatchedFileBuildItem item : items) {
                LOGGER.info("- {}", item.getLocation());
            }
        }

        return items;
    }
}
