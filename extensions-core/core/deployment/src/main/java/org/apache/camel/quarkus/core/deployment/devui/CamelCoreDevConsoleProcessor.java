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
package org.apache.camel.quarkus.core.deployment.devui;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import org.apache.camel.quarkus.devui.CamelCoreDevUIService;

@BuildSteps(onlyIf = IsDevelopment.class)
public class CamelCoreDevConsoleProcessor {

    @BuildStep
    void createDevUICards(BuildProducer<CardPageBuildItem> cardsProducer) {
        CardPageBuildItem cardPageBuildItem = new CardPageBuildItem();

        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("Blocked Exchanges")
                .icon("font-awesome-solid:circle-xmark")
                .componentLink("qwc-camel-core-blocked-exchanges.js"));

        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("Browse")
                .icon("font-awesome-solid:magnifying-glass")
                .componentLink("qwc-camel-core-browse.js"));

        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("Camel Context")
                .icon("font-awesome-solid:gear")
                .componentLink("qwc-camel-core-context.js"));

        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("Events")
                .icon("font-awesome-solid:bolt-lightning")
                .componentLink("qwc-camel-core-events.js"));

        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("Inflight Exchanges")
                .icon("font-awesome-solid:plane")
                .componentLink("qwc-camel-core-inflight-exchanges.js"));

        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("REST")
                .icon("font-awesome-solid:circle-nodes")
                .componentLink("qwc-camel-core-rest.js"));

        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("Routes")
                .icon("font-awesome-solid:route")
                .componentLink("qwc-camel-core-routes.js"));

        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("Variables")
                .icon("font-awesome-solid:code")
                .componentLink("qwc-camel-core-variables.js"));

        cardsProducer.produce(cardPageBuildItem);
    }

    @BuildStep
    JsonRPCProvidersBuildItem createJsonRPCServiceForCache() {
        return new JsonRPCProvidersBuildItem(CamelCoreDevUIService.class);
    }
}
