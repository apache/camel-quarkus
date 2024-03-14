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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SecretKeysHandler;
import io.smallrye.config.SecretKeysHandlerFactory;
import org.apache.camel.component.jasypt.JasyptPropertiesParser;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.StringHelper;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.EnvironmentStringPBEConfig;
import org.jasypt.iv.IvGenerator;
import org.jasypt.iv.NoIvGenerator;
import org.jasypt.iv.RandomIvGenerator;
import org.jasypt.salt.RandomSaltGenerator;
import org.jasypt.salt.SaltGenerator;

import static org.apache.camel.quarkus.component.jasypt.CamelJasyptConfig.DEFAULT_ALGORITHM;

public class CamelJasyptSecretKeysHandlerFactory implements SecretKeysHandlerFactory {
    private static final String CONFIG_PREFIX = "quarkus.camel.jasypt.";
    private static final String SYS_CONFIG_PREFIX = "sys:";
    private static final String SYS_ENV_CONFIG_PREFIX = "sysenv:";
    private static final Set<String> ALGORITHMS_THAT_REQUIRE_IV = Set.of(
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
    private final JasyptPropertiesParser parser = CamelJasyptPropertiesParserHolder.getJasyptPropertiesParser();
    private boolean enabled = true;

    @Override
    public SecretKeysHandler getSecretKeysHandler(ConfigSourceContext context) {
        String enabledValue = getConfigValue(context, "enabled", "true");
        if (enabledValue != null) {
            enabled = Boolean.parseBoolean(enabledValue);
        }

        if (enabled) {
            configureJasypt(context);
        }

        return new SecretKeysHandler() {
            @Override
            public String decode(String secret) {
                if (enabled) {
                    return parser.parseProperty("", secret, null);
                }
                return secret;
            }

            @Override
            public String getName() {
                return CamelJasyptConfig.NAME;
            }
        };
    }

    @Override
    public String getName() {
        return CamelJasyptConfig.NAME;
    }

    private void configureJasypt(ConfigSourceContext context) {
        EnvironmentStringPBEConfig config = new EnvironmentStringPBEConfig();

        String algorithm = getAlgorithm(context);
        config.setPassword(getPassword(context));
        config.setAlgorithm(algorithm);
        config.setIvGenerator(getIvGenerator(algorithm, context));
        config.setSaltGenerator(getSaltGenerator(context));
        customizeConfiguration(config, context);

        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setConfig(config);

        // Avoid potentially confusing runtime NPEs and fail fast if no password has been configured
        try {
            config.getPassword();
        } catch (NullPointerException e) {
            throw new IllegalStateException("The jasypt password has not been configured.");
        }

        CamelJasyptPropertiesParserHolder.setEncryptor(encryptor);
    }

    private String getPassword(ConfigSourceContext context) {
        Optional<String> passwordOptional = getOptionalConfigValue(context, "password");
        if (passwordOptional.isPresent()) {
            String password = passwordOptional.get();
            if (ObjectHelper.isNotEmpty(password)) {
                // Preserve backwards compat with the Camel way of configuring the master password
                if (password.startsWith(SYS_ENV_CONFIG_PREFIX)) {
                    password = System.getenv(StringHelper.after(password, SYS_ENV_CONFIG_PREFIX));
                } else if (password.startsWith(SYS_CONFIG_PREFIX)) {
                    password = System.getProperty(StringHelper.after(password, SYS_CONFIG_PREFIX));
                }
            }
            return password;
        }
        return null;
    }

    private String getAlgorithm(ConfigSourceContext context) {
        return getConfigValue(context, "algorithm", DEFAULT_ALGORITHM);
    }

    private IvGenerator getIvGenerator(String algorithm, ConfigSourceContext context) {
        if (ObjectHelper.isNotEmpty(algorithm) && ALGORITHMS_THAT_REQUIRE_IV.contains(algorithm.toUpperCase())) {
            String ivGeneratorAlgorithm = getConfigValue(context, "random-iv-generator-algorithm",
                    RandomSaltGenerator.DEFAULT_SECURE_RANDOM_ALGORITHM);
            return new RandomIvGenerator(ivGeneratorAlgorithm);
        }
        return new NoIvGenerator();
    }

    private SaltGenerator getSaltGenerator(ConfigSourceContext context) {
        String algorithm = getConfigValue(context, "random-salt-generator-algorithm",
                RandomSaltGenerator.DEFAULT_SECURE_RANDOM_ALGORITHM);
        return new RandomSaltGenerator(algorithm);
    }

    private void customizeConfiguration(EnvironmentStringPBEConfig config, ConfigSourceContext context) {
        Optional<String> customizerClassName = getOptionalConfigValue(context, "configuration-customizer-class-name");
        if (customizerClassName.isPresent()) {
            try {
                Class<?> encryptorClass = Thread.currentThread().getContextClassLoader().loadClass(customizerClassName.get());
                JasyptConfigurationCustomizer customizer = (JasyptConfigurationCustomizer) encryptorClass
                        .getDeclaredConstructor().newInstance();
                customizer.customize(config);
            } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException
                    | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String getConfigValue(ConfigSourceContext context, String key, String defaultValue) {
        String configKey = CONFIG_PREFIX + key;
        ConfigValue value = context.getValue(configKey);
        if (value != null) {
            return value.getValue();
        }
        if (defaultValue != null) {
            return defaultValue;
        }
        throw new NoSuchElementException("Property value for %s was not found".formatted(configKey));
    }

    private Optional<String> getOptionalConfigValue(ConfigSourceContext context, String key) {
        ConfigValue value = context.getValue(CONFIG_PREFIX + key);
        if (value != null) {
            return Optional.of(value.getValue());
        }
        return Optional.empty();
    }
}
