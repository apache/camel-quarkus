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

import java.util.OptionalInt;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigSourceInterceptorFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.Priorities;
import org.jasypt.properties.PropertyValueEncryptionUtils;

/**
 * Custom {@link ConfigSourceInterceptorFactory} to preserve the default camel-jasypt UX where a property value
 * can be defined like {@code secret.config=ENC(encrypted-string)}.
 * SmallRye config requires the secret key handler to be specified on the config value. This interceptor fulfils that
 * requirement for the user automatically.
 */
public class CamelJasyptConfigSourceInterceptorFactory implements ConfigSourceInterceptorFactory {
    @Override
    public ConfigSourceInterceptor getInterceptor(ConfigSourceInterceptorContext context) {
        return new ConfigSourceInterceptor() {
            @Override
            public ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {
                ConfigValue configValue = context.proceed(name);
                if (configValue != null) {
                    String value = configValue.getValue();
                    if (PropertyValueEncryptionUtils.isEncryptedValue(value)) {
                        return configValue.withValue("${camel-jasypt::%s}".formatted(value));
                    }
                }
                return configValue;
            }
        };
    }

    @Override
    public OptionalInt getPriority() {
        return OptionalInt.of(Priorities.LIBRARY);
    }
}
