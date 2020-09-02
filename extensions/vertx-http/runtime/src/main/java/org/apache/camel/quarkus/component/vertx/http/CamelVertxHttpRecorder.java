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
package org.apache.camel.quarkus.component.vertx.http;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import org.apache.camel.Exchange;
import org.apache.camel.component.vertx.http.DefaultVertxHttpBinding;
import org.apache.camel.component.vertx.http.VertxHttpComponent;
import org.apache.camel.spi.HeaderFilterStrategy;
import org.apache.camel.support.ExchangeHelper;
import org.apache.camel.util.ObjectHelper;

@Recorder
public class CamelVertxHttpRecorder {

    public RuntimeValue<?> createVertxHttpComponent(RuntimeValue<Vertx> vertx) {
        VertxHttpComponent component = new VertxHttpComponent();
        component.setVertx(vertx.getValue());
        component.setVertxHttpBinding(new QuarkusVertxHttpBinding());
        return new RuntimeValue<>(component);
    }

    // TODO: Remove when https://issues.apache.org/jira/browse/CAMEL-15495 is resolved
    static class QuarkusVertxHttpBinding extends DefaultVertxHttpBinding {
        @Override
        public void populateRequestHeaders(Exchange exchange, HttpRequest<Buffer> request, HeaderFilterStrategy strategy) {
            super.populateRequestHeaders(exchange, request, strategy);

            String contentType = ExchangeHelper.getContentType(exchange);
            if (ObjectHelper.isNotEmpty(contentType)) {
                request.putHeader(Exchange.CONTENT_TYPE, contentType);
            }
        }
    }
}
