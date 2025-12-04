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
package org.apache.camel.quarkus.component.milo.it;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import static org.apache.camel.component.milo.NodeIds.nodeValue;
import static org.apache.camel.component.milo.server.MiloServerComponent.DEFAULT_NAMESPACE_URI;
import static org.apache.camel.quarkus.component.milo.it.MiloProducers.CERT_KEYSTORE_PASSWORD;
import static org.apache.camel.quarkus.component.milo.it.MiloProducers.CERT_KEYSTORE_URL;

@ApplicationScoped
public class MiloRoutes extends RouteBuilder {
    public static final String SIMPLE_SERVER_ITEM_ID = "simple-server";
    public static final String SECURE_SERVER_ITEM_ID = "secure-server";
    public static final String SERVER_CREDENTIALS = "test:test";

    @ConfigProperty(name = "camel.component.milo-server.port")
    Integer port;

    @ConfigProperty(name = "camel.component.milo-secure-server.port")
    Integer securePort;

    @Override
    public void configure() throws Exception {
        fromF("milo-server:%s", SIMPLE_SERVER_ITEM_ID)
                .toF("seda:%s", SIMPLE_SERVER_ITEM_ID);

        fromF("milo-%s:%s", SECURE_SERVER_ITEM_ID, SECURE_SERVER_ITEM_ID)
                .toF("seda:%s", SECURE_SERVER_ITEM_ID);

        from("direct:sendToMilo")
                .toF("milo-client:opc.tcp://%s@localhost:%d?node=%s&overrideHost=true", SERVER_CREDENTIALS, port,
                        nodeValue(DEFAULT_NAMESPACE_URI, SIMPLE_SERVER_ITEM_ID));

        from("direct:sendToMiloSecure")
                .toF("milo-client:opc.tcp://%s@localhost:%d?node=%s&overrideHost=true&keyStoreUrl=%s&keyStorePassword=%s&keyPassword=%s",
                        SERVER_CREDENTIALS,
                        securePort,
                        nodeValue(DEFAULT_NAMESPACE_URI, SECURE_SERVER_ITEM_ID),
                        CERT_KEYSTORE_URL,
                        CERT_KEYSTORE_PASSWORD,
                        CERT_KEYSTORE_PASSWORD);
    }
}
