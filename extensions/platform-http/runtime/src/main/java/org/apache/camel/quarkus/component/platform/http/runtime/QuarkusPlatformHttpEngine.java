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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.component.platform.http.PlatformHttpEndpoint;
import org.apache.camel.component.platform.http.spi.PlatformHttpEngine;
import org.apache.camel.quarkus.core.UploadAttacher;


public class QuarkusPlatformHttpEngine implements PlatformHttpEngine {
    private final Router router;
    private final List<Handler<RoutingContext>> handlers;
    private final UploadAttacher uploadAttacher;

    public QuarkusPlatformHttpEngine(Router router, List<Handler<RoutingContext>> handlers, UploadAttacher uploadAttacher) {
        this.router = router;
        this.handlers = new ArrayList<>(handlers);
        this.uploadAttacher = uploadAttacher;
    }

    @Override
    public Consumer createConsumer(PlatformHttpEndpoint endpoint, Processor processor) {
        return new QuarkusPlatformHttpConsumer(endpoint, processor, router, handlers, uploadAttacher);
    }

    public List<Handler<RoutingContext>> getHandlers() {
        return Collections.unmodifiableList(this.handlers);
    }
}
