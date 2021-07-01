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

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import com.myjeeva.digitalocean.impl.DigitalOceanClient;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.ConfigProvider;
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
    @Named("digitalOceanClient")
    DigitalOceanClient initDigitalOceanClient(MockApiService mockApiService) {
        Optional<String> wireMockUrl = ConfigProvider.getConfig().getOptionalValue("wiremock.url", String.class);
        if (wireMockUrl.isPresent()) {
            return mockApiService.createDigitalOceanClient(wireMockUrl.get(), oAuthToken);
        }
        return new DigitalOceanClient(oAuthToken);
    }

    @Override
    public void configure() throws Exception {
        from("direct:droplet")
                .toF("digitalocean:droplets?oAuthToken=%s&digitalOceanClient=#digitalOceanClient", oAuthToken);

        from("direct:account")
                .toF("digitalocean:account?oAuthToken=%s&digitalOceanClient=#digitalOceanClient", oAuthToken);

        from("direct:actions")
                .toF("digitalocean:actions?oAuthToken=%s&digitalOceanClient=#digitalOceanClient", oAuthToken);

        from("direct:images")
                .toF("digitalocean:images?oAuthToken=%s&digitalOceanClient=#digitalOceanClient", oAuthToken);

        from("direct:snapshots")
                .toF("digitalocean:snapshots?oAuthToken=%s&digitalOceanClient=#digitalOceanClient", oAuthToken);

        from("direct:sizes")
                .toF("digitalocean:sizes?oAuthToken=%s&digitalOceanClient=#digitalOceanClient", oAuthToken);

        from("direct:regions")
                .toF("digitalocean:regions?oAuthToken=%s&digitalOceanClient=#digitalOceanClient", oAuthToken);

        from("direct:floatingIPs")
                .toF("digitalocean:floatingIPs?oAuthToken=%s&digitalOceanClient=#digitalOceanClient", oAuthToken);

        from("direct:blockStorages")
                .toF("digitalocean:blockStorages?oAuthToken=%s&digitalOceanClient=#digitalOceanClient", oAuthToken);

        from("direct:keys")
                .toF("digitalocean:keys?oAuthToken=%s&digitalOceanClient=#digitalOceanClient", oAuthToken);

        from("direct:tags")
                .toF("digitalocean:tags?oAuthToken=%s&digitalOceanClient=#digitalOceanClient", oAuthToken);
    }
}
