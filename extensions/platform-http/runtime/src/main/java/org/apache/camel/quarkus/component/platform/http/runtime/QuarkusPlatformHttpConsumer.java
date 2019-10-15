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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import org.apache.camel.Consumer;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.NoTypeConversionAvailableException;
import org.apache.camel.Processor;
import org.apache.camel.TypeConversionException;
import org.apache.camel.TypeConverter;
import org.apache.camel.component.platform.http.PlatformHttpComponent;
import org.apache.camel.component.platform.http.PlatformHttpEndpoint;
import org.apache.camel.component.platform.http.spi.Method;
import org.apache.camel.spi.HeaderFilterStrategy;
import org.apache.camel.support.DefaultConsumer;
import org.apache.camel.support.DefaultMessage;
import org.apache.camel.support.ExchangeHelper;
import org.apache.camel.support.MessageHelper;
import org.apache.camel.support.ObjectHelper;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

/**
 * A Quarkus specific {@link Consumer} for the {@link PlatformHttpComponent}.
 */
public class QuarkusPlatformHttpConsumer extends DefaultConsumer {
    private static final Logger LOG = Logger.getLogger(QuarkusPlatformHttpConsumer.class);

    private final Router router;
    private Route route;

    public QuarkusPlatformHttpConsumer(Endpoint endpoint, Processor processor, Router router) {
        super(endpoint, processor);
        this.router = router;
    }

    @Override
    public PlatformHttpEndpoint getEndpoint() {
        return (PlatformHttpEndpoint) super.getEndpoint();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        final String path = getEndpoint().getPath();
        final Route newRoute = router.route(path);

        final Set<Method> methods = Method.parseList(getEndpoint().getHttpMethodRestrict());
        if (!methods.equals(Method.getAll())) {
            methods.stream().forEach(m -> newRoute.method(HttpMethod.valueOf(m.name())));
        }

        Config cfg = ConfigProvider.getConfig();
        final BodyHandler bodyHandler = BodyHandler.create();
        /*
         * Keep in sync with how the BodyHandler is configured in io.quarkus.vertx.web.runtime.VertxWebRecorder
         * Eventually, VertxWebRecorder should have a method to do this for us.
         *
         * TODO: remove this code when moving to quarkus 0.24.x, see https://github.com/quarkusio/quarkus/pull/4314
         */
        cfg.getOptionalValue("quarkus.http.body.handle-file-uploads", boolean.class).ifPresent(bodyHandler::setHandleFileUploads);
        cfg.getOptionalValue("quarkus.http.body.uploads-directory", String.class).ifPresent(bodyHandler::setUploadsDirectory);
        cfg.getOptionalValue("quarkus.http.body.delete-uploaded-files-on-end", boolean.class).ifPresent(bodyHandler::setDeleteUploadedFilesOnEnd);
        cfg.getOptionalValue("quarkus.http.body.merge-form-attributes", boolean.class).ifPresent(bodyHandler::setMergeFormAttributes);
        cfg.getOptionalValue("quarkus.http.body.preallocate-body-buffer", boolean.class).ifPresent(bodyHandler::setPreallocateBodyBuffer);

        newRoute
            //
            // This should not be needed but because the default route added by quarkus (i.e. in case
            // the quarkus-resteasy extension is in the classpath) is a catch all, it is required to
            // configure the route to be evaluated before the default one.
            //
            // TODO: remove this after https://github.com/quarkusio/quarkus/issues/4407 is fixed.
            //
            .order(-1)
            .handler(bodyHandler)
            .handler(ctx -> {
                try {
                    final PlatformHttpEndpoint endpoint = getEndpoint();
                    final HeaderFilterStrategy headerFilterStrategy = endpoint.getHeaderFilterStrategy();
                    final Exchange e = toExchange(ctx, endpoint.createExchange(), headerFilterStrategy);
                    getProcessor().process(e);
                    writeResponse(ctx, e, headerFilterStrategy);
                } catch (Exception e1) {
                    LOG.debugf(e1, "Could not handle '%s'", path);
                    ctx.fail(e1);
                }
            });

        this.route = newRoute;
    }

    @Override
    protected void doStop() throws Exception {
        if (route != null) {
            route.remove();
            route = null;
        }
        super.doStop();
    }

    @Override
    protected void doSuspend() throws Exception {
        if (route != null) {
            route.disable();
        }
        super.doSuspend();
    }

    @Override
    protected void doResume() throws Exception {
        if (route != null) {
            route.enable();
        }
        super.doResume();
    }

    static Object toHttpResponse(HttpServerResponse response, Message message, HeaderFilterStrategy headerFilterStrategy) {
        final Exchange exchange = message.getExchange();
        final boolean failed = exchange.isFailed();
        final int defaultCode = failed ? 500 : 200;

        final int code = message.getHeader(Exchange.HTTP_RESPONSE_CODE, defaultCode, int.class);

        response.setStatusCode(code);

        final TypeConverter tc = exchange.getContext().getTypeConverter();

        //copy headers from Message to Response
        if (headerFilterStrategy != null) {
            for (Map.Entry<String, Object> entry : message.getHeaders().entrySet()) {
                final String key = entry.getKey();
                final Object value = entry.getValue();
                // use an iterator as there can be multiple values. (must not use a delimiter)
                final Iterator<?> it = ObjectHelper.createIterator(value, null);
                String firstValue = null;
                List<String> values = null;
                while (it.hasNext()) {
                    final String headerValue = tc.convertTo(String.class, it.next());
                    if (headerValue != null
                            && !headerFilterStrategy.applyFilterToCamelHeaders(key, headerValue, exchange)) {
                        if (firstValue == null) {
                            firstValue = headerValue;
                        } else {
                            if (values == null) {
                                values = new ArrayList<String>();
                                values.add(firstValue);
                            }
                            values.add(headerValue);
                        }
                    }
                }
                if (values != null) {
                    response.putHeader(key, values);
                } else if (firstValue != null) {
                    response.putHeader(key, firstValue);
                }
            }
        }

        Object body = message.getBody();
        final Exception exception = exchange.getException();

        if (exception != null) {
            // we failed due an exception so print it as plain text
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            exception.printStackTrace(pw);

            // the body should then be the stacktrace
            body = ByteBuffer.wrap(sw.toString().getBytes(StandardCharsets.UTF_8));
            // force content type to be text/plain as that is what the stacktrace is
            message.setHeader(Exchange.CONTENT_TYPE, "text/plain; charset=utf-8");

            // and mark the exception as failure handled, as we handled it by returning it as the response
            ExchangeHelper.setFailureHandled(exchange);
        }

        // set the content type in the response.
        final String contentType = MessageHelper.getContentType(message);
        if (contentType != null) {
            // set content-type
            response.putHeader("Content-Type", contentType);
        }
        return body;
    }

    static void writeResponse(RoutingContext ctx, Exchange camelExchange, HeaderFilterStrategy headerFilterStrategy) {
        final Object body = toHttpResponse(ctx.response(), camelExchange.getMessage(), headerFilterStrategy);

        final HttpServerResponse response = ctx.response();
        if (body == null) {
            LOG.tracef("No payload to send as reply for exchange: %s", camelExchange);
            response.putHeader("Content-Type", "text/plain; charset=utf-8");
            response.end("No response available");
            return;
        }

        if (body instanceof String) {
            response.end((String) body);
        } else if (body instanceof InputStream) {
            final byte[] bytes = new byte[4096];
            try (InputStream in = (InputStream) body) {
                int len;
                while ((len = in.read(bytes))  >= 0) {
                    final Buffer b = Buffer.buffer(len);
                    b.appendBytes(bytes, 0, len);
                    response.write(b);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            response.end();
        } else {
            final TypeConverter tc = camelExchange.getContext().getTypeConverter();
            try {
                final ByteBuffer bb = tc.mandatoryConvertTo(ByteBuffer.class, body);
                final Buffer b = Buffer.buffer(bb.capacity());
                b.setBytes(0, bb);
                response.end(b);
            } catch (TypeConversionException | NoTypeConversionAvailableException e) {
                throw new RuntimeException(e);
            }
        }

    }

    static Exchange toExchange(RoutingContext ctx, Exchange exchange, HeaderFilterStrategy headerFilterStrategy) {
        Message in = toCamelMessage(ctx, exchange, headerFilterStrategy);

        final String charset = ctx.parsedHeaders().contentType().parameter("charset");
        if (charset != null) {
            exchange.setProperty(Exchange.CHARSET_NAME, charset);
            in.setHeader(Exchange.HTTP_CHARACTER_ENCODING, charset);
        }

        exchange.setIn(in);
        return exchange;
    }

    static void populateCamelHeaders(RoutingContext ctx, Map<String, Object> headersMap, Exchange exchange,
            HeaderFilterStrategy headerFilterStrategy) {

        final HttpServerRequest request = ctx.request();
        headersMap.put(Exchange.HTTP_PATH, request.path());

        if (headerFilterStrategy != null) {
            final MultiMap requestHeaders = request.headers();
            final String authz = requestHeaders.get("authorization");
            // store a special header that this request was authenticated using HTTP Basic
            if (authz != null && authz.trim().startsWith("Basic")) {
                if (!headerFilterStrategy.applyFilterToExternalHeaders(Exchange.AUTHENTICATION, "Basic", exchange)) {
                    appendHeader(headersMap, Exchange.AUTHENTICATION, "Basic");
                }
            }
            for (String name : requestHeaders.names()) {
                // add the headers one by one, and use the header filter strategy
                for (String value : requestHeaders.getAll(name)) {
                    if (!headerFilterStrategy.applyFilterToExternalHeaders(name.toString(), value, exchange)) {
                        appendHeader(headersMap, name.toString(), value);
                    }
                }
            }

            // process uri parameters as headers
            final MultiMap pathParameters = ctx.queryParams();
            // continue if the map is not empty, otherwise there are no params
            if (!pathParameters.isEmpty()) {
                for (String name : pathParameters.names()) {
                    for (String value : pathParameters.getAll(name)) {
                        if (!headerFilterStrategy.applyFilterToExternalHeaders(name, value, exchange)) {
                            appendHeader(headersMap, name, value);
                        }
                    }
                }
            }
        }

        // TODO: figure out whether we need this or remove
        //        // Create headers for REST path placeholder variables
        //        Map<String, Object> predicateContextParams = httpExchange.getAttachment(Predicate.PREDICATE_CONTEXT);
        //        if (predicateContextParams != null) {
        //            // Remove this as it's an unwanted artifact of our Undertow predicate chain
        //            predicateContextParams.remove("remaining");
        //
        //            for (String paramName : predicateContextParams.keySet()) {
        //                headersMap.put(paramName, predicateContextParams.get(paramName));
        //            }
        //        }

        // NOTE: these headers is applied using the same logic as camel-http/camel-jetty to be consistent
        headersMap.put(Exchange.HTTP_METHOD, request.method().toString());
        // strip query parameters from the uri
        headersMap.put(Exchange.HTTP_URL, request.absoluteURI());
        // uri is without the host and port
        headersMap.put(Exchange.HTTP_URI, request.uri());
        headersMap.put(Exchange.HTTP_QUERY, request.query());
        headersMap.put(Exchange.HTTP_RAW_QUERY, request.query());
    }

    static Message toCamelMessage(RoutingContext ctx, Exchange exchange, HeaderFilterStrategy headerFilterStrategy) {
        Message result = new DefaultMessage(exchange);

        populateCamelHeaders(ctx, result.getHeaders(), exchange, headerFilterStrategy);
        final String mimeType = ctx.parsedHeaders().contentType().value();
        if ("application/x-www-form-urlencoded".equals(mimeType) || "multipart/form-data".equals(mimeType)) {
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
            result.setBody(body);
        } else {
            //extract body by myself if undertow parser didn't handle and the method is allowed to have one
            //body is extracted as byte[] then auto TypeConverter kicks in
            Method m = Method.valueOf(ctx.request().method().name());
            if (m.canHaveBody()) {
                final Buffer body = ctx.getBody();
                result.setBody(body.getBytes());
            } else {
                result.setBody(null);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    static void appendHeader(Map<String, Object> headers, String key, Object value) {
        if (headers.containsKey(key)) {
            Object existing = headers.get(key);
            List<Object> list;
            if (existing instanceof List) {
                list = (List<Object>) existing;
            } else {
                list = new ArrayList<>();
                list.add(existing);
            }
            list.add(value);
            value = list;
        }

        headers.put(key, value);
    }

}