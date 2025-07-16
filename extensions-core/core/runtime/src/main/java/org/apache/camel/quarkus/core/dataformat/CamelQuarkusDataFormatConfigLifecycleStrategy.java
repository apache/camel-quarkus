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
package org.apache.camel.quarkus.core.dataformat;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.camel.CamelContext;
import org.apache.camel.Ordered;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.support.LifecycleStrategySupport;
import org.apache.camel.support.PropertyBindingSupport;
import org.eclipse.microprofile.config.ConfigProvider;

public class CamelQuarkusDataFormatConfigLifecycleStrategy extends LifecycleStrategySupport implements Ordered {
    private final static Pattern KEBAB_CASE_PATTERN = Pattern.compile("-([a-z])");
    private final CamelContext camelContext;
    private final CamelDataFormatRuntimeConfig config;

    public CamelQuarkusDataFormatConfigLifecycleStrategy(CamelContext camelContext, CamelDataFormatRuntimeConfig config) {
        this.camelContext = camelContext;
        this.config = config;
    }

    @Override
    public void onDataFormatCreated(String name, DataFormat dataFormat) {
        if (config.dataFormatConfigs().containsKey(name) && isCustomizersEnabled()) {
            // Convert the config keys to camel case for property binding
            Map<String, Object> camelCaseMap = config.dataFormatConfigs()
                    .get(name)
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            entry -> KEBAB_CASE_PATTERN.matcher(entry.getKey()).replaceAll(mr -> mr.group(1).toUpperCase()),
                            Map.Entry::getValue));

            PropertyBindingSupport.build().withIgnoreCase(true).bind(camelContext, dataFormat, camelCaseMap);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST;
    }

    static boolean isCustomizersEnabled() {
        Boolean dataFormatCustomizerEnabled = ConfigProvider.getConfig()
                .getOptionalValue("camel.component.dataformat.customizer.enabled", boolean.class)
                .orElse(true);

        Boolean componentCustomizerEnabled = ConfigProvider.getConfig()
                .getOptionalValue("camel.component.customizer.enabled", boolean.class)
                .orElse(true);

        return dataFormatCustomizerEnabled && componentCustomizerEnabled;
    }
}
