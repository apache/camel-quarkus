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

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.SecretKeysHandler;
import io.smallrye.config.SecretKeysHandlerFactory;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.apache.camel.component.jasypt.JasyptPropertiesParser;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

public class CamelJasyptSecretKeysHandlerFactory implements SecretKeysHandlerFactory {
    private final JasyptPropertiesParser parser = CamelJasyptPropertiesParserHolder.getJasyptPropertiesParser();

    @Override
    public SecretKeysHandler getSecretKeysHandler(ConfigSourceContext context) {
        return new LazySecretKeysHandler(new SecretKeysHandlerFactory() {
            @Override
            public SecretKeysHandler getSecretKeysHandler(final ConfigSourceContext context) {
                SmallRyeConfig config = new SmallRyeConfigBuilder()
                        .withSources(new ConfigSourceContext.ConfigSourceContextConfigSource(context))
                        .withMapping(CamelJasyptConfig.class)
                        .withMapping(CamelJasyptBuildTimeConfig.class)
                        .build();

                CamelJasyptConfig jasyptConfig = config.getConfigMapping(CamelJasyptConfig.class);
                StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
                encryptor.setConfig(jasyptConfig.pbeConfig());
                CamelJasyptPropertiesParserHolder.setEncryptor(encryptor);

                return new SecretKeysHandler() {
                    @Override
                    public String decode(String secret) {
                        return parser.parseProperty("", secret, null);
                    }

                    @Override
                    public String getName() {
                        return CamelJasyptSecretKeysHandlerFactory.this.getName();
                    }
                };
            }

            @Override
            public String getName() {
                return CamelJasyptSecretKeysHandlerFactory.this.getName();
            }
        });
    }

    @Override
    public String getName() {
        return CamelJasyptConfig.NAME;
    }
}
