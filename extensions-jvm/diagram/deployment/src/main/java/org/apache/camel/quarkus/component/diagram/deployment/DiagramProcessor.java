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
package org.apache.camel.quarkus.component.diagram.deployment;

import java.util.function.Consumer;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import org.apache.camel.quarkus.component.diagram.CamelDiagramRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelContextBuildItem;

@BuildSteps(onlyIf = IsDevelopment.class)
class DiagramProcessor {
    private static final String FEATURE = "camel-diagram";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void createDiagramRoute(
            CamelContextBuildItem camelContext,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            BuildProducer<RouteBuildItem> routes,
            CamelDiagramRecorder recorder) {

        Consumer<Route> route = recorder.route();
        Handler<RoutingContext> handler = recorder.getHandler(camelContext.getCamelContext());

        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .management()
                .routeFunction("camel/diagram/:id", route)
                .handler(handler)
                .build());
    }
}
