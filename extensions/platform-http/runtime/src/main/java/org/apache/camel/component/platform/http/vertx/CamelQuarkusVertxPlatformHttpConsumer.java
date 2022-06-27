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
package org.apache.camel.component.platform.http.vertx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.component.platform.http.PlatformHttpEndpoint;
import org.apache.camel.component.platform.http.spi.Method;
import org.apache.camel.spi.HeaderFilterStrategy;

import static org.apache.camel.component.platform.http.vertx.VertxPlatformHttpSupport.appendHeader;
import static org.apache.camel.component.platform.http.vertx.VertxPlatformHttpSupport.populateCamelHeaders;

// TODO: Remove when Camel / Quarkus Vert.x version is in sync https://github.com/apache/camel-quarkus/issues/3877
public final class CamelQuarkusVertxPlatformHttpConsumer extends VertxPlatformHttpConsumer {

    public CamelQuarkusVertxPlatformHttpConsumer(PlatformHttpEndpoint endpoint, Processor processor,
            List<Handler<RoutingContext>> handlers) {
        super(endpoint, processor, handlers);
    }

    @Override
    protected Message toCamelMessage(RoutingContext ctx, Exchange exchange) {
        final Message result = exchange.getIn();

        final HeaderFilterStrategy headerFilterStrategy = getEndpoint().getHeaderFilterStrategy();
        populateCamelHeaders(ctx, result.getHeaders(), exchange, headerFilterStrategy);
        final String mimeType = ctx.parsedHeaders().contentType().value();
        final boolean isMultipartFormData = "multipart/form-data".equals(mimeType);
        if ("application/x-www-form-urlencoded".equals(mimeType) || isMultipartFormData) {
            final MultiMap formData = ctx.request().formAttributes();
            final Map<String, Object> body = new HashMap<>();
            for (String key : formData.names()) {
                for (String value : formData.getAll(key)) {
                    if (headerFilterStrategy != null
                            && !headerFilterStrategy.applyFilterToExternalHeaders(key, value, exchange)) {
                        appendHeader(result.getHeaders(), key, value);
                        appendHeader(body, key, value);
                    }
                }
            }

            if (!body.isEmpty()) {
                result.setBody(body);
            }

            if (isMultipartFormData) {
                populateAttachments(new ArrayList<>(ctx.fileUploads()), result);
            }
        } else {
            Method m = Method.valueOf(ctx.request().method().name());
            if (m.canHaveBody()) {
                final Buffer body = ctx.getBody();
                if (body != null) {
                    result.setBody(body);
                } else {
                    result.setBody(null);
                }
            } else {
                result.setBody(null);
            }
        }
        return result;
    }
}
