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
package org.apache.camel.quarkus.component.jfr;

import java.util.OptionalInt;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigSourceInterceptorFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.Priorities;

/**
 * Map {@link RuntimeCamelJfrConfig} startupRecorder properties to their equivalents in camel main.
 */
public class CamelJfrConfigSourceInterceptorFactory implements ConfigSourceInterceptorFactory {

    private static final String PROP_PREFIX_CAMEL_JFR = "quarkus.camel.jfr";
    private static final String PROP_PREFIX_CAMEL_MAIN = "camel.main";

    @Override
    public ConfigSourceInterceptor getInterceptor(ConfigSourceInterceptorContext configSourceInterceptorContext) {
        return new ConfigSourceInterceptor() {
            @Override
            public ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {
                ConfigValue value = context.proceed(name);

                // If no quarkus.camel.jfr property value set, see if there's an equivalent camel.main property
                if (name.startsWith(PROP_PREFIX_CAMEL_JFR) && value == null) {
                    String property = name.substring(name.lastIndexOf("."));

                    ConfigValue camelMainValue = context.proceed(PROP_PREFIX_CAMEL_MAIN + property);
                    if (camelMainValue != null) {
                        return camelMainValue;
                    }

                    camelMainValue = context.proceed(PROP_PREFIX_CAMEL_MAIN + property);
                    return camelMainValue;
                }

                return value;
            }
        };
    }

    @Override
    public OptionalInt getPriority() {
        return OptionalInt.of(Priorities.LIBRARY + 1000);
    }
}
