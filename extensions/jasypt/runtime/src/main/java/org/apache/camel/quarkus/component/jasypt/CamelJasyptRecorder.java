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

import java.util.regex.Pattern;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.config.SmallRyeConfig;
import org.apache.camel.CamelContext;
import org.apache.camel.component.jasypt.JasyptPropertiesParser;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.main.BaseMainSupport;
import org.apache.camel.main.MainConfigurationProperties;
import org.apache.camel.main.MainListener;
import org.apache.camel.spi.CamelContextCustomizer;
import org.apache.camel.util.ObjectHelper;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

@Recorder
public class CamelJasyptRecorder {
    private static final Pattern JASYPT_ENC = Pattern.compile("ENC\\(\\s*\\S.*\\)");

    public RuntimeValue<MainListener> createDisabledCamelMainAutoConfigFromSysEnvMainListener() {
        return new RuntimeValue<>(new MainListener() {
            @Override
            public void beforeInitialize(BaseMainSupport main) {

            }

            @Override
            public void beforeConfigure(BaseMainSupport main) {
                MainConfigurationProperties configurationProperties = main.configure();
                configurationProperties.setAutoConfigurationSystemPropertiesEnabled(false);
                configurationProperties.setAutoConfigurationEnvironmentVariablesEnabled(false);
            }

            @Override
            public void afterConfigure(BaseMainSupport main) {

            }

            @Override
            public void beforeStart(BaseMainSupport main) {

            }

            @Override
            public void afterStart(BaseMainSupport main) {

            }

            @Override
            public void beforeStop(BaseMainSupport main) {

            }

            @Override
            public void afterStop(BaseMainSupport main) {

            }
        });
    }

    public RuntimeValue<CamelContextCustomizer> createPropertiesComponentCamelContextCustomizer() {
        return new RuntimeValue<>(new CamelContextCustomizer() {
            @Override
            public void configure(CamelContext camelContext) {
                // Since CamelJasyptSecretKeysHandlerFactory relies on LazySecretKeysHandler, we need to avoid
                // prematurely setting up Jasypt if there are no ENC(..) property values.
                // Otherwise, the Jasypt configuration will never get triggered.
                if (isAnyEncryptedPropertyPresent()) {
                    PropertiesComponent component = (PropertiesComponent) camelContext.getPropertiesComponent();
                    JasyptPropertiesParser jasyptPropertiesParser = CamelJasyptPropertiesParserHolder
                            .getJasyptPropertiesParser();
                    jasyptPropertiesParser.setPropertiesComponent(component);
                    component.setPropertiesParser(jasyptPropertiesParser);
                }
            }
        });
    }

    private boolean isAnyEncryptedPropertyPresent() {
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        for (ConfigSource configSource : config.getConfigSources()) {
            for (String value : configSource.getProperties().values()) {
                if (ObjectHelper.isNotEmpty(value) && JASYPT_ENC.matcher(value).matches()) {
                    return true;
                }
            }
        }
        return false;
    }
}
