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
package org.apache.camel.quarkus.component.vertx.websocket;

import io.opentelemetry.api.trace.SpanContext;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.RoutingContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.vertx.websocket.VertxWebsocketConsumer;
import org.apache.camel.component.vertx.websocket.VertxWebsocketEndpoint;
import org.apache.camel.component.vertx.websocket.VertxWebsocketEvent;

/**
 * Custom Vert.x WebSocket consumer for Quarkus that captures the HTTP upgrade span context
 * and makes it available for OpenTelemetry tracing with span links.
 */
public class QuarkusVertxWebsocketConsumer extends VertxWebsocketConsumer {

    /**
     * Exchange header name for storing the HTTP upgrade span context.
     * This allows the OpenTelemetry tracer to create span links back to the original HTTP request.
     * Format: "traceId:spanId" (hex strings separated by colon)
     */
    public static final String PROPERTY_HANDSHAKE_SPAN_CONTEXT = "CamelVertxWebsocketHandshakeSpanContext";

    public QuarkusVertxWebsocketConsumer(VertxWebsocketEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
    }

    @Override
    protected void populateExchangeHeaders(
            Exchange exchange, String connectionKey, SocketAddress remote, RoutingContext routingContext,
            VertxWebsocketEvent event) {
        super.populateExchangeHeaders(exchange, connectionKey, remote, routingContext, event);

        SpanContext handshakeSpanContext = routingContext.get(PROPERTY_HANDSHAKE_SPAN_CONTEXT);
        if (handshakeSpanContext != null && handshakeSpanContext.isValid()) {
            String serialized = handshakeSpanContext.getTraceId() + ":" + handshakeSpanContext.getSpanId();
            exchange.getIn().setHeader(PROPERTY_HANDSHAKE_SPAN_CONTEXT, serialized);
        }
    }
}
