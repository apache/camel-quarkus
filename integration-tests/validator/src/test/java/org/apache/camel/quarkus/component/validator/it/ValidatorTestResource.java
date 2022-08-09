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
package org.apache.camel.quarkus.component.validator.it;

import java.util.HashMap;
import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

public class ValidatorTestResource implements QuarkusTestResourceLifecycleManager {

    private WireMockServer server;

    @Override
    public Map<String, String> start() {

        String responseBody = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" "
                + "  elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">"
                + "<xs:element name=\"message\">"
                + "<xs:complexType>"
                + "<xs:sequence>"
                + "<xs:element name=\"firstName\" type=\"xs:string\"/>"
                + "<xs:element name=\"lastName\" type=\"xs:string\"/>"
                + "</xs:sequence>"
                + "</xs:complexType>"
                + "</xs:element>"
                + "</xs:schema>";

        server = new WireMockServer(WireMockConfiguration.DYNAMIC_PORT);
        server.start();
        server.stubFor(
                get(urlEqualTo("/xsd"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/XML")
                                .withBody(responseBody)));
        Map<String, String> conf = new HashMap<>();
        conf.put("xsd.server-url", server.baseUrl());
        return conf;
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop();
        }
    }

}
