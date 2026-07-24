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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import org.apache.camel.quarkus.devui.CamelCoreDevUIRecorder;
import org.apache.camel.quarkus.devui.CamelCoreDevUIService;

@BuildSteps(onlyIf = IsDevelopment.class)
public class CamelCoreDevConsoleProcessor {

    @BuildStep
    void createDevUICards(BuildProducer<CardPageBuildItem> cardsProducer) {
        CardPageBuildItem cardPageBuildItem = new CardPageBuildItem();

        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("Blocked Exchanges")
                .icon("font-awesome-solid:circle-xmark")
                .componentLink("qwc-camel-core-blocked-exchanges.js")
                .metadata(CamelDevUIConstants.CONSOLE_ID_METADATA_KEY, "blocked"));

        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("Browse")
                .icon("font-awesome-solid:magnifying-glass")
                .componentLink("qwc-camel-core-browse.js")
                .metadata(CamelDevUIConstants.CONSOLE_ID_METADATA_KEY, "browse")
                .metadata(CamelDevUIConstants.ALLOWED_OPTIONS_METADATA_KEY, "dump=false"));

        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("Camel Context")
                .icon("font-awesome-solid:gear")
                .componentLink("qwc-camel-core-context.js")
                .metadata(CamelDevUIConstants.CONSOLE_ID_METADATA_KEY, "context"));

        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("Diagram")
                .icon("font-awesome-solid:sitemap")
                .componentLink("qwc-camel-core-diagram.js")
                .metadata(CamelDevUIConstants.CONSOLE_ID_METADATA_KEY, CamelDevUIConstants.CONSOLE_ID_NONE));

        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("Events")
                .icon("font-awesome-solid:bolt-lightning")
                .componentLink("qwc-camel-core-events.js")
                .metadata(CamelDevUIConstants.CONSOLE_ID_METADATA_KEY, "event"));

        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("Inflight Exchanges")
                .icon("font-awesome-solid:plane")
                .componentLink("qwc-camel-core-inflight-exchanges.js")
                .metadata(CamelDevUIConstants.CONSOLE_ID_METADATA_KEY, "inflight"));

        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("REST")
                .icon("font-awesome-solid:circle-nodes")
                .componentLink("qwc-camel-core-rest.js")
                .metadata(CamelDevUIConstants.CONSOLE_ID_METADATA_KEY, "rest"));

        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("Routes")
                .icon("font-awesome-solid:route")
                .componentLink("qwc-camel-core-routes.js")
                .metadata(CamelDevUIConstants.CONSOLE_ID_METADATA_KEY, "route")
                .metadata(CamelDevUIConstants.ALLOWED_OPTIONS_METADATA_KEY, "limit=*"));

        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("Variables")
                .icon("font-awesome-solid:code")
                .componentLink("qwc-camel-core-variables.js")
                .metadata(CamelDevUIConstants.CONSOLE_ID_METADATA_KEY, "variables"));

        cardsProducer.produce(cardPageBuildItem);
    }

    @BuildStep
    JsonRPCProvidersBuildItem createJsonRPCServiceForCache() {
        return new JsonRPCProvidersBuildItem(CamelCoreDevUIService.class);
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void recordAllowedConsoleMetadata(
            CamelCoreDevUIRecorder recorder,
            List<CardPageBuildItem> cardPages) {
        Set<String> allowedIds = new HashSet<>();
        Map<String, String> allowedOptions = new HashMap<>();

        cardPages.stream()
                .flatMap(card -> card.getPages().stream())
                .forEach(pageBuilder -> {
                    Page page = pageBuilder.build();
                    String componentLink = page.getComponentLink();
                    boolean isCamelPage = componentLink != null && componentLink.startsWith("qwc-camel");
                    if (!isCamelPage) {
                        return;
                    }

                    Map<String, String> metadata = page.getMetadata();
                    boolean hasConsoleId = metadata != null
                            && metadata.containsKey(CamelDevUIConstants.CONSOLE_ID_METADATA_KEY);
                    if (!hasConsoleId) {
                        throw new IllegalStateException(
                                "Camel Dev UI page '" + componentLink
                                        + "' is missing required metadata key '" + CamelDevUIConstants.CONSOLE_ID_METADATA_KEY
                                        + "'. Set it to the Camel dev console id, or to CamelDevUIConstants.CONSOLE_ID_NONE"
                                        + " if the page does not use the JSON-RPC console bridge.");
                    }

                    String consoleId = metadata.get(CamelDevUIConstants.CONSOLE_ID_METADATA_KEY);
                    if (!CamelDevUIConstants.CONSOLE_ID_NONE.equals(consoleId)) {
                        allowedIds.add(consoleId);
                        if (metadata.containsKey(CamelDevUIConstants.ALLOWED_OPTIONS_METADATA_KEY)) {
                            allowedOptions.put(consoleId, metadata.get(CamelDevUIConstants.ALLOWED_OPTIONS_METADATA_KEY));
                        }
                    }
                });

        recorder.setAllowedConsoleIds(allowedIds);
        recorder.setAllowedConsoleOptions(allowedOptions);
    }
}
