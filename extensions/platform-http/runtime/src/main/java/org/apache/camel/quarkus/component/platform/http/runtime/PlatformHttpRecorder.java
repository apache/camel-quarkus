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
package org.apache.camel.quarkus.component.platform.http.runtime;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.ext.web.Router;

import org.apache.camel.component.platform.http.PlatformHttpComponent;
import org.apache.camel.component.platform.http.PlatformHttpConstants;
import org.apache.camel.component.platform.http.spi.PlatformHttpEngine;
import org.apache.camel.quarkus.core.runtime.CamelRuntime;
import org.apache.camel.spi.Registry;

@Recorder
public class PlatformHttpRecorder {

    public void registerPlatformHttpComponent(RuntimeValue<CamelRuntime> runtime, RuntimeValue<Router> router) {
        final Registry registry = runtime.getValue().getRegistry();
        final PlatformHttpEngine engine = new QuarkusPlatformHttpEngine(router.getValue());
        registry.bind(PlatformHttpConstants.PLATFORM_HTTP_ENGINE_NAME, PlatformHttpEngine.class, engine);

        final PlatformHttpComponent component = new PlatformHttpComponent(runtime.getValue().getContext());
        registry.bind(PlatformHttpConstants.PLATFORM_HTTP_COMPONENT_NAME, PlatformHttpComponent.class, component);
    }

}
