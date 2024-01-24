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
package org.apache.camel.quarkus.component.jasypt;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import org.jasypt.encryption.pbe.StandardPBEByteEncryptor;
import org.jasypt.iv.RandomIvGenerator;
import org.jasypt.salt.RandomSaltGenerator;

/**
 * Note: This class exists mainly for documentation purposes. The actual configuration values
 * are read via the SmallRye config internals within the SecretKeysHandler.
 */
@ConfigRoot(name = "camel.jasypt", phase = ConfigPhase.RUN_TIME)
public class CamelJasyptConfig {
    static final String NAME = "camel-jasypt";
    static final String DEFAULT_ALGORITHM = StandardPBEByteEncryptor.DEFAULT_ALGORITHM;

    /**
     * Setting this option to false will disable Jasypt integration with Quarkus SmallRye configuration.
     * You can however, manually configure Jasypt with Camel in the 'classic' way of manually configuring
     * JasyptPropertiesParser and PropertiesComponent. Refer to the usage section for more details.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * The algorithm to be used for decryption.
     */
    @ConfigItem(defaultValue = DEFAULT_ALGORITHM)
    public String algorithm;

    /**
     * The master password used by Jasypt for decrypting configuration values.
     * This option supports prefixes which influence the master password lookup behaviour.
     * <p>
     * <code>sys:</code> will to look up the value from a JVM system property.
     * <code>sysenv:</code> will look up the value from the OS system environment with the given key.
     * <p>
     */
    @ConfigItem
    public Optional<String> password;

    /**
     * Configures the Jasypt StandardPBEStringEncryptor with a RandomIvGenerator using the given algorithm.
     */
    @ConfigItem(defaultValue = RandomIvGenerator.DEFAULT_SECURE_RANDOM_ALGORITHM)
    public String randomIvGeneratorAlgorithm;

    /**
     * Configures the Jasypt StandardPBEStringEncryptor with a RandomSaltGenerator using the given algorithm.
     */
    @ConfigItem(defaultValue = RandomSaltGenerator.DEFAULT_SECURE_RANDOM_ALGORITHM)
    public String randomSaltGeneratorAlgorithm;

    /**
     * The fully qualified class name of an org.apache.camel.quarkus.component.jasypt.JasyptConfigurationCustomizer
     * implementation. This provides the optional capability of having full control over the Jasypt configuration.
     */
    @ConfigItem
    public Optional<String> configurationCustomizerClassName;
}
