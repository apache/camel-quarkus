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
package org.apache.camel.quarkus.component.log.it;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.log.LogComponent;
import org.apache.camel.quarkus.main.events.AfterStart;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
@Path("/log")
public class LogResource {
    @Inject
    ProducerTemplate producerTemplate;

    @ConfigProperty(name = "quarkus.http.test-port")
    Optional<Integer> httpTestPort;
    @ConfigProperty(name = "quarkus.http.port")
    Optional<Integer> httpPort;

    private int getEffectivePort() {
        Optional<Integer> portSource = LogUtils.isNativeMode() ? httpPort : httpTestPort;
        return portSource.orElse(0);
    }

    public void afterApplicationStartup(@Observes AfterStart event) {
        try {
            Files.writeString(LogUtils.resolveQuarkusLogPath(),
                    "Listening on: http://0.0.0.0:" + getEffectivePort() + System.lineSeparator());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @jakarta.enterprise.inject.Produces
    @Named("mdcLog")
    LogComponent logComponent() {
        // Use a new LogComponent instance because when we run tests 'grouped',
        // integration-test-customized-log-component introduces a custom exchangeFormatter
        // which messes up the log output expected by the MDC tests
        LogComponent logComponent = new LogComponent();
        logComponent.setCamelContext(producerTemplate.getCamelContext());
        return logComponent;
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String logMessage(@QueryParam("endpointUri") String endpointUri, String message) {
        Exchange result = producerTemplate.request(endpointUri, exchange -> exchange.getMessage().setBody(message));
        return result.getExchangeId();
    }
}
