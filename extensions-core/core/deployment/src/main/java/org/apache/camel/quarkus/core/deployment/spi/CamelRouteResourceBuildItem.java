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
package org.apache.camel.quarkus.core.deployment.spi;

import java.nio.file.Paths;
import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;
import org.apache.camel.quarkus.core.util.FileUtils;
import org.apache.camel.spi.Resource;
import org.apache.camel.util.FileUtil;
import org.apache.camel.util.StringHelper;

import static org.apache.camel.quarkus.core.deployment.util.CamelSupport.CLASSPATH_PREFIX;

/**
 * Holds a {@link Resource} relating to discovered Camel DSL route definition files defined by
 * route inclusion patterns configuration.
 */
public final class CamelRouteResourceBuildItem extends MultiBuildItem {
    private static final String GRADLE_RESOURCES_PATH = "build/resources/main/";
    private static final String MAVEN_CLASSES_PATH = "target/classes/";
    private final String location;
    private final String sourcePath;
    private final boolean isHotReloadable;

    public CamelRouteResourceBuildItem(String location) {
        this(location, true);
    }

    public CamelRouteResourceBuildItem(String location, boolean isHotReloadable) {
        Objects.requireNonNull(location, "location cannot be null");
        this.location = FileUtils.nixifyPath(location);
        this.sourcePath = computeSourcePath();
        this.isHotReloadable = isHotReloadable;
    }

    public String getLocation() {
        return location;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public boolean isHotReloadable() {
        return isHotReloadable;
    }

    public boolean isClasspathResource() {
        return location.startsWith(CLASSPATH_PREFIX)
                || location.contains(MAVEN_CLASSES_PATH)
                || location.contains(GRADLE_RESOURCES_PATH);
    }

    private String computeSourcePath() {
        String result = StringHelper.after(location, ":", location);

        if (location.startsWith("file:")) {
            return Paths.get(result).toAbsolutePath().toString();
        }

        result = FileUtil.stripLeadingSeparator(result);

        if (location.contains(MAVEN_CLASSES_PATH)) {
            result = StringHelper.after(location, MAVEN_CLASSES_PATH);
        }

        if (location.contains(GRADLE_RESOURCES_PATH)) {
            result = StringHelper.after(location, GRADLE_RESOURCES_PATH);
        }

        return result;
    }
}
