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
package org.apache.camel.quarkus.component.opentracing.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.opentracing.Span;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.opentracing.OpenTracingSpanAdapter;
import org.apache.camel.tracing.ActiveSpanManager;

@ApplicationScoped
public class OpenTracingRouteBuilder extends RouteBuilder {

    @Inject
    TracedBean tracedBean;

    @Override
    public void configure() throws Exception {
        from("platform-http:/opentracing/test/trace?httpMethodRestrict=GET")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
                .setBody(constant("GET: /opentracing/test/trace"));

        from("platform-http:/opentracing/test/trace/filtered")
                .setBody(constant("GET: /opentracing/test/trace/filtered"));

        from("platform-http:/opentracing/test/bean")
                .process(exchange -> {
                    Span span = getCurrentSpan(exchange);
                    tracedBean.doTrace(span);
                })
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200));

        from("direct:start")
                .process(exchange -> {
                    Span span = getCurrentSpan(exchange);
                    tracedBean.doTrace(span);
                })
                .setBody().constant("Traced direct:start");
    }

    private Span getCurrentSpan(Exchange exchange) {
        OpenTracingSpanAdapter adapter = (OpenTracingSpanAdapter) ActiveSpanManager.getSpan(exchange);
        return adapter.getOpenTracingSpan();
    }
}
