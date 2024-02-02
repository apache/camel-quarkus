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
package org.apache.camel.quarkus.component.jasypt.deployment.devui;

import java.util.function.BooleanSupplier;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import org.apache.camel.quarkus.component.jasypt.CamelJasyptBuildTimeConfig;
import org.apache.camel.quarkus.component.jasypt.CamelJasyptDevUIService;

@BuildSteps(onlyIf = { IsDevelopment.class, JasyptUtilsDevUIProcessor.CamelJasyptEnabled.class })
public class JasyptUtilsDevUIProcessor {
    @BuildStep
    CardPageBuildItem create() {
        CardPageBuildItem card = new CardPageBuildItem();
        card.addPage(Page.webComponentPageBuilder()
                .title("Utilities")
                .componentLink("qwc-camel-jasypt-utils.js")
                .icon("font-awesome-solid:house-lock"));
        return card;
    }

    @BuildStep
    JsonRPCProvidersBuildItem createJsonRPCServiceForCache() {
        return new JsonRPCProvidersBuildItem(CamelJasyptDevUIService.class);
    }

    static final class CamelJasyptEnabled implements BooleanSupplier {
        CamelJasyptBuildTimeConfig config;

        @Override
        public boolean getAsBoolean() {
            return config.enabled;
        }
    }
}
