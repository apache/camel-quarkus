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
package org.apache.camel.quarkus.component.http.common;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.FluentProducerTemplate;

@ApplicationScoped
public abstract class AbstractHttpResource {
    public static final String PROXIED_URL = "https://repo.maven.apache.org/maven2/org/apache/camel/quarkus/camel-quarkus-%s/maven-metadata.xml";
    public static final String USER_ADMIN = "admin";
    public static final String USER_ADMIN_PASSWORD = "adm1n";
    public static final String USER_NO_ADMIN = "noadmin";
    public static final String USER_NO_ADMIN_PASSWORD = "n0Adm1n";

    @Inject
    protected FluentProducerTemplate producerTemplate;

    @Inject
    protected ConsumerTemplate consumerTemplate;

    @Path("/auth/basic/secured")
    @GET
    @RolesAllowed("admin")
    @Produces(MediaType.TEXT_PLAIN)
    public String basicAuth(@QueryParam("component") String component) {
        return "Component " + component + " is using basic auth";
    }

    public abstract String httpGet(int port);

    public abstract String httpPost(int port, String message);

    @Path("/get-https")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String httpsGet(@QueryParam("component") String component) {
        return producerTemplate
                .toF("direct:%s-https", component)
                .withHeader(Exchange.HTTP_METHOD, "GET")
                .request(String.class);
    }

    public abstract Response basicAuth(int port, String username, String password);

    public abstract String httpProxy();
}
