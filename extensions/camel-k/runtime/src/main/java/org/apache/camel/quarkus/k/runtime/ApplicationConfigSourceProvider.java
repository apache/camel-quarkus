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
package org.apache.camel.quarkus.k.runtime;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import io.smallrye.config.PropertiesConfigSource;
import org.apache.camel.component.kubernetes.properties.ConfigMapPropertiesFunction;
import org.apache.camel.util.ObjectHelper;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationConfigSourceProvider implements ConfigSourceProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfigSourceProvider.class);

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        final Map<String, String> sysProperties = new HashMap<>();
        // explicit disable looking up configmap and secret using the KubernetesClient
        sysProperties.put(ConfigMapPropertiesFunction.CLIENT_ENABLED, "false");

        final Map<String, String> appProperties = loadApplicationProperties();

        // As today the resources directory is part of the conf.d directory, ideally the resources
        // directory should be moved outside it, so we can walk the conf.d directory without having
        // to individually load sub-dirs.

        final Map<String, String> usrPropertiesConfigMaps = loadConfigMapUserProperties();
        final Map<String, String> usrPropertiesSecrets = loadSecretsProperties();
        final Map<String, String> abBindingProperties = loadServiceBindingsProperties();

        return List.of(
                new PropertiesConfigSource(
                        sysProperties,
                        "camel-k-sys",
                        ConfigSource.DEFAULT_ORDINAL + 1100),
                new PropertiesConfigSource(
                        appProperties,
                        "camel-k-app",
                        ConfigSource.DEFAULT_ORDINAL + 1050),
                new PropertiesConfigSource(
                        usrPropertiesConfigMaps,
                        "camel-k-usr-configmap",
                        ConfigSource.DEFAULT_ORDINAL + 1010),
                new PropertiesConfigSource(
                        usrPropertiesSecrets,
                        "camel-k-usr-secrets",
                        ConfigSource.DEFAULT_ORDINAL + 1010),
                new PropertiesConfigSource(
                        abBindingProperties,
                        "camel-k-servicebindings",
                        ConfigSource.DEFAULT_ORDINAL + 1005));
    }

    public static Map<String, String> loadApplicationProperties() {
        final String conf = System.getProperty(ApplicationConstants.PROPERTY_CAMEL_K_CONF,
                System.getenv(ApplicationConstants.ENV_CAMEL_K_CONF));
        final Map<String, String> properties = new HashMap<>();

        if (ObjectHelper.isEmpty(conf)) {
            return properties;
        }

        try {
            Path confPath = Paths.get(conf);
            if (Files.exists(confPath) && !Files.isDirectory(confPath)) {
                try (Reader reader = Files.newBufferedReader(confPath)) {
                    Properties p = new Properties();
                    p.load(reader);
                    p.forEach((key, value) -> properties.put(String.valueOf(key), String.valueOf(value)));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return properties;
    }

    public static Map<String, String> loadConfigMapUserProperties() {
        return loadUserProperties(
                ApplicationConstants.PROPERTY_CAMEL_K_MOUNT_PATH_CONFIGMAPS,
                ApplicationConstants.ENV_CAMEL_K_MOUNT_PATH_CONFIGMAPS,
                ApplicationConstants.PATH_CONFIGMAPS);
    }

    public static Map<String, String> loadSecretsProperties() {
        return loadUserProperties(
                ApplicationConstants.PROPERTY_CAMEL_K_MOUNT_PATH_SECRETS,
                ApplicationConstants.ENV_CAMEL_K_MOUNT_PATH_SECRETS,
                ApplicationConstants.PATH_SECRETS);
    }

    public static Map<String, String> loadServiceBindingsProperties() {
        return loadUserProperties(
                ApplicationConstants.PROPERTY_CAMEL_K_MOUNT_PATH_SERVICEBINDINGS,
                ApplicationConstants.ENV_CAMEL_K_MOUNT_PATH_SERVICEBINDINGS,
                ApplicationConstants.PATH_SERVICEBINDINGS);
    }

    public static Map<String, String> loadUserProperties(String property, String env, String subpath) {
        String path = System.getProperty(property, System.getenv(env));

        if (path == null) {
            String conf = System.getProperty(
                    ApplicationConstants.PROPERTY_CAMEL_K_CONF_D,
                    System.getenv(ApplicationConstants.ENV_CAMEL_K_CONF_D));

            if (conf != null) {
                if (!conf.endsWith("/")) {
                    conf = conf + "/";
                }

                path = conf + subpath;
            }
        }

        if (ObjectHelper.isEmpty(path)) {
            return Map.of();
        }

        final Map<String, String> properties = new HashMap<>();
        final Path root = Paths.get(path);

        if (Files.exists(root)) {
            FileVisitor<Path> visitor = propertiesCollector(properties);
            try {
                Files.walkFileTree(root, visitor);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return Collections.unmodifiableMap(properties);
    }

    private static FileVisitor<Path> propertiesCollector(Map<String, String> properties) {
        return new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Objects.requireNonNull(file);
                Objects.requireNonNull(attrs);

                if (Files.isDirectory(file) || Files.isSymbolicLink(file)) {
                    return FileVisitResult.CONTINUE;
                }

                if (file.toFile().getAbsolutePath().endsWith(".properties")) {
                    try (Reader reader = Files.newBufferedReader(file)) {
                        Properties p = new Properties();
                        p.load(reader);
                        p.forEach((key, value) -> properties.put(String.valueOf(key), String.valueOf(value)));
                    }
                } else {
                    try {
                        properties.put(
                                file.getFileName().toString(),
                                Files.readString(file, StandardCharsets.UTF_8));
                    } catch (MalformedInputException mie) {
                        // Just skip if it is not a UTF-8 encoded file (ie a binary)
                        LOGGER.info("Cannot transform {} into UTF-8 text, skipping.", file);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        };
    }
}
