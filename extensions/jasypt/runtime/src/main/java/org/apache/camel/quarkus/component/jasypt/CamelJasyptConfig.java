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

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.StringHelper;
import org.jasypt.encryption.pbe.StandardPBEByteEncryptor;
import org.jasypt.encryption.pbe.config.EnvironmentStringPBEConfig;
import org.jasypt.encryption.pbe.config.PBEConfig;
import org.jasypt.iv.NoIvGenerator;
import org.jasypt.iv.RandomIvGenerator;
import org.jasypt.salt.RandomSaltGenerator;

@ConfigMapping(prefix = "quarkus.camel.jasypt")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface CamelJasyptConfig {
    String NAME = "camel-jasypt";
    String DEFAULT_ALGORITHM = StandardPBEByteEncryptor.DEFAULT_ALGORITHM;

    /**
     * The algorithm to be used for decryption.
     */
    @WithDefault(DEFAULT_ALGORITHM)
    String algorithm();

    /**
     * The master password used by Jasypt for decrypting configuration values.
     * This option supports prefixes which influence the master password lookup behaviour.
     * <p>
     * <code>sys:</code> will to look up the value from a JVM system property.
     * <code>sysenv:</code> will look up the value from the OS system environment with the given key.
     * <p>
     */
    Optional<String> password();

    /**
     * Configures the Jasypt StandardPBEStringEncryptor with a RandomIvGenerator using the given algorithm.
     */
    @WithDefault(RandomIvGenerator.DEFAULT_SECURE_RANDOM_ALGORITHM)
    String randomIvGeneratorAlgorithm();

    /**
     * Configures the Jasypt StandardPBEStringEncryptor with a RandomSaltGenerator using the given algorithm.
     */
    @WithDefault(RandomSaltGenerator.DEFAULT_SECURE_RANDOM_ALGORITHM)
    String randomSaltGeneratorAlgorithm();

    /**
     * The fully qualified class name of an org.apache.camel.quarkus.component.jasypt.JasyptConfigurationCustomizer
     * implementation. This provides the optional capability of having full control over the Jasypt configuration.
     */
    Optional<String> configurationCustomizerClassName();

    String SYS_CONFIG_PREFIX = "sys:";
    String SYS_ENV_CONFIG_PREFIX = "sysenv:";
    Set<String> ALGORITHMS_THAT_REQUIRE_IV = Set.of(
            "PBEWITHHMACSHA1ANDAES_128",
            "PBEWITHHMACSHA1ANDAES_256",
            "PBEWITHHMACSHA224ANDAES_128",
            "PBEWITHHMACSHA224ANDAES_256",
            "PBEWITHHMACSHA256ANDAES_128",
            "PBEWITHHMACSHA256ANDAES_256",
            "PBEWITHHMACSHA384ANDAES_128",
            "PBEWITHHMACSHA384ANDAES_256",
            "PBEWITHHMACSHA512ANDAES_128",
            "PBEWITHHMACSHA512ANDAES_256");

    default PBEConfig pbeConfig() {
        EnvironmentStringPBEConfig config = new EnvironmentStringPBEConfig();

        String password = null;
        if (password().isPresent()) {
            password = password().get();
            if (ObjectHelper.isNotEmpty(password)) {
                // Preserve backwards compat with the Camel way of configuring the master password
                if (password.startsWith(SYS_ENV_CONFIG_PREFIX)) {
                    password = System.getenv(StringHelper.after(password, SYS_ENV_CONFIG_PREFIX));
                } else if (password.startsWith(SYS_CONFIG_PREFIX)) {
                    password = System.getProperty(StringHelper.after(password, SYS_CONFIG_PREFIX));
                }
            }
        }

        config.setPassword(password);
        config.setAlgorithm(algorithm());
        config.setIvGenerator(ALGORITHMS_THAT_REQUIRE_IV.contains(algorithm().toUpperCase())
                ? new RandomIvGenerator(randomIvGeneratorAlgorithm()) : new NoIvGenerator());
        config.setSaltGenerator(new RandomSaltGenerator(randomSaltGeneratorAlgorithm()));

        if (configurationCustomizerClassName().isPresent()) {
            try {
                Class<?> encryptorClass = Thread.currentThread().getContextClassLoader()
                        .loadClass(configurationCustomizerClassName().get());
                JasyptConfigurationCustomizer customizer = (JasyptConfigurationCustomizer) encryptorClass
                        .getDeclaredConstructor().newInstance();
                customizer.customize(config);
            } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException
                    | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        // Avoid potentially confusing runtime NPEs and fail fast if no password has been configured
        try {
            config.getPassword();
        } catch (NullPointerException e) {
            throw new IllegalStateException("The jasypt password has not been configured.");
        }

        return config;
    }
}
