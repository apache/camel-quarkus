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
package org.apache.camel.quarkus.core.events;

import org.apache.camel.ErrorHandlerFactory;
import org.apache.camel.Processor;
import org.apache.camel.Route;
import org.apache.camel.impl.event.AbstractRouteEvent;
import org.apache.camel.spi.CamelEvent;

/**
 * Base {@link CamelEvent} for {@link ErrorHandlerFactory} related events.
 */
public class ErrorHandlerEvent extends AbstractRouteEvent {
    private final Processor errorHandler;
    private final ErrorHandlerFactory errorHandlerFactory;

    public ErrorHandlerEvent(Route route, Processor errorHandler, ErrorHandlerFactory errorHandlerFactory) {
        super(route);
        this.errorHandler = errorHandler;
        this.errorHandlerFactory = errorHandlerFactory;
    }

    public Processor getErrorHandler() {
        return errorHandler;
    }

    public ErrorHandlerFactory getErrorHandlerFactory() {
        return errorHandlerFactory;
    }

    @Override
    public Type getType() {
        return Type.Custom;
    }
}
