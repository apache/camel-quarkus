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
package org.apache.camel.quarkus.component.oaipmh.it;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformerV2;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.apache.camel.quarkus.test.AvailablePortFinder;
import org.apache.camel.quarkus.test.support.certificate.CertificatesUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public final class MockOaipmhServer {

    private static Map<String, String> responseCache;
    static final String PASSWORD = "changeit";

    private int httpPort;
    private int httpsPort;
    private WireMockServer server;

    private MockOaipmhServer(int httpPort, int httpsPort) {
        this.httpPort = httpPort;
        this.httpsPort = httpsPort;
    }

    public static MockOaipmhServer create() {
        int httpPort = AvailablePortFinder.getNextAvailable();
        int httpsPort = AvailablePortFinder.getNextAvailable();
        MockOaipmhServer server = new MockOaipmhServer(httpPort, httpsPort);
        return server;
    }

    /**
     * Gets the response cache from the mocked data ZIP file.
     */
    private static synchronized Map<String, String> getResponseCache() {
        try {
            if (responseCache == null) {
                HashMap<String, String> cache = new HashMap<String, String>();

                ZipInputStream zis = new ZipInputStream(MockOaipmhServer.class.getResourceAsStream("/data.zip"));

                ZipEntry entry = zis.getNextEntry();
                while (entry != null) {
                    if (!entry.isDirectory()) {
                        cache.put(StringUtils.substringAfterLast(entry.getName(), "/"),
                                IOUtils.toString(zis, StandardCharsets.UTF_8));
                    }
                    entry = zis.getNextEntry();
                }
                responseCache = Collections.unmodifiableMap(cache);
            }
        } catch (IOException ioex) {
            throw new RuntimeException("An issue occurred while initializing the OAI-PMH mock server response cache", ioex);
        }
        return responseCache;
    }

    public void start() {
        OaipmhMockTransformer transformer = new OaipmhMockTransformer();
        WireMockConfiguration config = wireMockConfig().extensions(transformer);

        config.httpsPort(httpsPort);
        config.keystorePath(CertificatesUtil.keystoreFile("oaipmh", "p12"));
        config.keystorePassword(PASSWORD);
        config.keyManagerPassword(PASSWORD);

        config.port(httpPort);

        server = new WireMockServer(config);
        server.start();
    }

    public void stop() {
        server.stop();
        AvailablePortFinder.releaseReservedPorts();
    }

    public int getHttpPort() {
        return this.httpPort;
    }

    public int getHttpsPort() {
        return this.httpsPort;
    }

    public static final class OaipmhMockTransformer implements ResponseDefinitionTransformerV2 {
        @Override
        public ResponseDefinition transform(ServeEvent event) {
            String sha256Hex = DigestUtils.sha256Hex(event.getRequest().getUrl());
            return new ResponseDefinitionBuilder().withStatus(200).withBody(getResponseCache().get(sha256Hex + ".xml")).build();
        }

        @Override
        public String getName() {
            return "oaipmh-mock-transformer";
        }
    }
}
