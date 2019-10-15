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
package org.apache.camel.component.platform.http;

import org.apache.camel.AsyncEndpoint;
import org.apache.camel.Component;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.component.platform.http.spi.PlatformHttpEngine;
import org.apache.camel.spi.HeaderFilterStrategy;
import org.apache.camel.spi.HeaderFilterStrategyAware;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@UriEndpoint(/* firstVersion = "3.?.0", */ scheme = "platform-http", title = "Platform HTTP", syntax = "platform-http:path", label = "http", consumerOnly = true)
public class PlatformHttpEndpoint extends DefaultEndpoint implements AsyncEndpoint, HeaderFilterStrategyAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformHttpEndpoint.class);

    @UriPath
    @Metadata(required = true)
    private final String path;

    @UriParam(label = "consumer", description = "A comma separated list of HTTP methods to serve, e.g. GET,POST ."
            + " If no methods are specified, all methods will be served.")
    private String httpMethodRestrict;

    @UriParam(label = "advanced")
    private PlatformHttpEngine platformHttpEngine;

    @UriParam(label = "advanced")
    private HeaderFilterStrategy headerFilterStrategy = new PlatformHttpHeaderFilterStrategy();

    public PlatformHttpEndpoint(String uri, String remaining, Component component) {
        super(uri, component);
        path = remaining;
    }

    @Override
    public Producer createProducer() throws Exception {
        throw new UnsupportedOperationException("Producer is not supported");
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        return platformHttpEngine.createConsumer(this, processor);
    }

    @Override
    public HeaderFilterStrategy getHeaderFilterStrategy() {
        return headerFilterStrategy;
    }

    @Override
    public void setHeaderFilterStrategy(HeaderFilterStrategy headerFilterStrategy) {
        this.headerFilterStrategy = headerFilterStrategy;
    }

    public String getPath() {
        return path;
    }

    public PlatformHttpEngine getPlatformHttpEngine() {
        return platformHttpEngine;
    }

    public void setPlatformHttpEngine(PlatformHttpEngine platformHttpEngine) {
        this.platformHttpEngine = platformHttpEngine;
    }

    public String getHttpMethodRestrict() {
        return httpMethodRestrict;
    }

    public void setHttpMethodRestrict(String httpMethodRestrict) {
        this.httpMethodRestrict = httpMethodRestrict;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        if (platformHttpEngine == null) {
            LOGGER.debug("Lookup platform http engine from registry");

            platformHttpEngine = getCamelContext().getRegistry()
                    .lookupByNameAndType(PlatformHttpConstants.PLATFORM_HTTP_ENGINE_NAME, PlatformHttpEngine.class);

            if (platformHttpEngine == null) {
                throw new IllegalStateException(PlatformHttpEngine.class.getSimpleName() + " neither set on this "
                        + PlatformHttpEndpoint.class.getSimpleName() + " neither found in Camel Registry.");
            }
        }
    }
}
