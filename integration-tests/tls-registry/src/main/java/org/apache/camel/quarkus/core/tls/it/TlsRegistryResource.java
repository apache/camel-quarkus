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

import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/tls-registry")
@ApplicationScoped
public class TlsRegistryResource {

    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate producerTemplate;

    @ConfigProperty(name = "quarkus.http.ssl-port", defaultValue = "8443")
    int httpsPort;

    @Path("/beans")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> getSSLBeans() {
        return context.getRegistry().findByTypeWithName(SSLContextParameters.class).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> "SSLContextParameters"));
    }

    @Path("/bean/exists/{name}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public boolean beanExists(@PathParam("name") String name) {
        return context.getRegistry().lookupByNameAndType(name, SSLContextParameters.class) != null;
    }

    @Path("/global-ssl")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public boolean hasGlobalSSL() {
        return context.getSSLContextParameters() != null;
    }

    @Path("/ping")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String ping() {
        return "pong";
    }

    @Path("/http/call-with-named-ssl/{sslBeanName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String callWithNamedSsl(@PathParam("sslBeanName") String sslBeanName) {
        String host = "localhost:" + httpsPort + "/tls-registry/ping";
        return producerTemplate.requestBodyAndHeaders("direct:https-with-named-ssl",
                null,
                Map.of("host", host, "sslBeanName", sslBeanName),
                String.class);
    }

    @Path("/http/call-with-global-ssl")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String callWithGlobalSsl() {
        String host = "localhost:" + httpsPort + "/tls-registry/ping";
        return producerTemplate.requestBodyAndHeader("direct:https-with-global-ssl",
                null,
                "host", host,
                String.class);
    }
}
