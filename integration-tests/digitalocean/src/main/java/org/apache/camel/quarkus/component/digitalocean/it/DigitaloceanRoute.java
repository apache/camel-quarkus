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
package org.apache.camel.quarkus.component.digitalocean.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.ws.rs.Produces;

import com.myjeeva.digitalocean.impl.DigitalOceanClient;
import io.quarkus.arc.Unremovable;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class DigitaloceanRoute extends RouteBuilder {
    @ConfigProperty(name = "DIGITALOCEAN_AUTH_TOKEN", defaultValue = "NONE")
    String oAuthToken;

    /**
     * We need to implement some conditional configuration of the {@link DigitalOceanClient} thus we create it
     * programmatically
     *
     * @return a configured {@link DigitalOceanClient}
     */
    @Produces
    @ApplicationScoped
    @Unremovable
    @Named("digitalOceanClient")
    DigitalOceanClient initDigitalOceanClient(MockApiService mockApiService) {
        final String wireMockUrl = System.getProperty("wiremock.url.ssl");
        if (wireMockUrl != null) {
            return mockApiService.createDigitalOceanClient(wireMockUrl, oAuthToken);
        }
        return new DigitalOceanClient(oAuthToken);
    }

    @Override
    public void configure() throws Exception {
        from("direct:droplet")
                .toF("digitalocean:droplets?oAuthToken=%s&digitalOceanClient=#digitalOceanClient", oAuthToken);

    }
}
