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
package org.apache.camel.quarkus.core.tls.it;

import java.util.concurrent.atomic.AtomicInteger;

import io.quarkus.tls.CertificateUpdatedEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.event.CamelContextReloadedEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/tls-registry/reload")
@ApplicationScoped
public class TlsRegistryReloadResource {

    @Inject
    Event<CertificateUpdatedEvent> certificateEventProducer;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext camelContext;

    @ConfigProperty(name = "quarkus.http.ssl-port", defaultValue = "8444")
    int httpsPort;

    private final AtomicInteger reloadCount = new AtomicInteger(0);

    /**
     * Observes CamelContextReloadedEvent and counts reloads.
     */
    void onContextReloaded(@Observes CamelContextReloadedEvent event) {
        reloadCount.incrementAndGet();
    }

    /**
     * Fires a certificate updated event.
     */
    @POST
    @Path("/fire-event/{certName}")
    @Produces(MediaType.TEXT_PLAIN)
    public String fireCertificateEvent(@PathParam("certName") String certName) {
        certificateEventProducer.fire(new CertificateUpdatedEvent(certName, null));
        return "Event fired for: " + certName;
    }

    /**
     * Returns the number of context reloads that have occurred.
     */
    @GET
    @Path("/count")
    @Produces(MediaType.TEXT_PLAIN)
    public String getReloadCount() {
        return String.valueOf(reloadCount.get());
    }

    /**
     * Resets the reload counter.
     */
    @POST
    @Path("/reset")
    @Produces(MediaType.TEXT_PLAIN)
    public String resetCounter() {
        reloadCount.set(0);
        return "Counter reset";
    }

    /**
     * Makes an HTTPS call using the default TLS configuration.
     * This verifies that SSL/TLS is working correctly by calling back to the Quarkus HTTPS server.
     */
    @GET
    @Path("/test-https")
    @Produces(MediaType.TEXT_PLAIN)
    public String testHttpsConnection() {
        try {
            String url = "https://localhost:" + httpsPort
                    + "/tls-registry/ping?sslContextParameters=#defaultSslContextParameters";
            String result = producerTemplate.requestBody(url, null, String.class);
            return result != null ? result : "No response";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
}
