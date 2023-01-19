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
package org.apache.camel.quarkus.support.language.deployment.dm;

import org.apache.camel.Category;
import org.apache.camel.Consumer;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.support.DefaultConsumer;
import org.apache.camel.support.DefaultEndpoint;
import org.apache.camel.support.DefaultProducer;

/**
 * {@code DryModeEndpoint} is a mock endpoint that is used as replacement of all endpoints in the routes for a dry run.
 */
@UriEndpoint(firstVersion = "3.20.0", scheme = "dm", title = "Dry Mode", syntax = "dm:name", category = { Category.CORE,
        Category.TESTING }, lenientProperties = true)
public class DryModeEndpoint extends DefaultEndpoint {

    private final String uri;

    public DryModeEndpoint(String uri) {
        this.uri = uri;
    }

    @Override
    public Producer createProducer() {
        return new DryModeProducer(this);
    }

    @Override
    public Consumer createConsumer(Processor processor) {
        return new DryModeConsumer(this, processor);
    }

    @Override
    protected String createEndpointUri() {
        return String.format("dm:%s", uri);
    }

    private static class DryModeConsumer extends DefaultConsumer {

        DryModeConsumer(Endpoint endpoint, Processor processor) {
            super(endpoint, processor);
        }
    }

    private static class DryModeProducer extends DefaultProducer {

        DryModeProducer(Endpoint endpoint) {
            super(endpoint);
        }

        @Override
        public void process(Exchange exchange) {
            // Nothing to do
        }
    }
}
