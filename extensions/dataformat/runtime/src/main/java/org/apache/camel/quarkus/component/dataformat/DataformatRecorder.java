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
package org.apache.camel.quarkus.component.dataformat;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.CamelContext;
import org.apache.camel.Ordered;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.support.LifecycleStrategySupport;
import org.apache.camel.support.PropertyBindingSupport;
import org.eclipse.microprofile.config.ConfigProvider;

@Recorder
public class DataformatRecorder {
    public void createDataformatCustomizer(final RuntimeValue<CamelContext> camelContext, final CamelDataformatConfig config) {
        camelContext.getValue().addLifecycleStrategy(new BeanioLifecycleStrategy(config, camelContext));
    }

    private static class BeanioLifecycleStrategy extends LifecycleStrategySupport implements Ordered {

        //kebab-case pattern
        private final static Pattern KEBAB_CASE_PATTERN = Pattern.compile("-([a-z])");

        private final CamelDataformatConfig config;
        private final RuntimeValue<CamelContext> camelContext;

        public BeanioLifecycleStrategy(CamelDataformatConfig config, RuntimeValue<CamelContext> camelContext) {
            this.config = config;
            this.camelContext = camelContext;
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST;
        }

        @Override
        public void onDataFormatCreated(String name, DataFormat dataFormat) {

            if (ConfigProvider.getConfig().getOptionalValue("camel.component.dataformat.customizer.enabled", Boolean.class)
                    .orElse(true)
                    && ConfigProvider.getConfig().getOptionalValue("camel.component.customizer.enabled", Boolean.class)
                            .orElse(true)) {

                //set properties from application.properties
                Map<String, String> properties = config.dataformats().get(name);

                if (properties != null) {
                    //convert properties to camel Case
                    Map<String, Object> camelCaseMap = properties.entrySet().stream().collect(Collectors.toMap(
                            entry -> KEBAB_CASE_PATTERN.matcher(entry.getKey()).replaceAll(mr -> mr.group(1).toUpperCase()),
                            Map.Entry::getValue));
                    PropertyBindingSupport.build().withIgnoreCase(true).bind(camelContext.getValue(), dataFormat, camelCaseMap);
                }
            }
        }
    }
}
