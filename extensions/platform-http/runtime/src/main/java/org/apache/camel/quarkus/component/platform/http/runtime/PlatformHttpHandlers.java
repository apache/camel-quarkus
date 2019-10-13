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


import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public final class PlatformHttpHandlers {
    private PlatformHttpHandlers() {
    }

    public static class Resumer implements Handler<RoutingContext> {
        @Override
        public void handle(RoutingContext context) {
            // Workaround for route paused when resteasy is added to the game
            // on quarkus >= 0.24.0
            context.request().resume();
            context.next();
        }
    }
}
