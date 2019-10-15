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
import org.apache.camel.component.platform.http.spi.PlatformHttpEngine;

@Recorder
public class PlatformHttpRecorder {
    public RuntimeValue<PlatformHttpEngine> createEngine(RuntimeValue<Router> router) {
        return new RuntimeValue<>(new QuarkusPlatformHttpEngine(router.getValue()));
    }

    public RuntimeValue<PlatformHttpComponent> createComponent(RuntimeValue<PlatformHttpEngine> engine) {
        PlatformHttpComponent component = new PlatformHttpComponent();
        component.setEngine(engine.getValue());

        return new RuntimeValue<>(component);
    }
}
