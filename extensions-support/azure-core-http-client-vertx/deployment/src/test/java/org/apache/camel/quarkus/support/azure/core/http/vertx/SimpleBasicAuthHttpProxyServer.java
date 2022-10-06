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
package org.apache.camel.quarkus.support.azure.core.http.vertx;

import java.util.Base64;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;

/**
 * A simple Http proxy server that enforce basic proxy authentication, once authenticated
 * any request matching {@code serviceEndpoints} will be responded with an empty Http 200.
 */
final class SimpleBasicAuthHttpProxyServer {
    private final String userName;
    private final String password;
    private final String[] serviceEndpoints;
    private WireMockServer proxyService;

    /**
     * Creates SimpleBasicAuthHttpProxyServer.
     *
     * @param userName         the proxy user name for basic authentication
     * @param password         the proxy password for basic authentication
     * @param serviceEndpoints the whitelisted mock endpoints targeting the service behind proxy
     */
    SimpleBasicAuthHttpProxyServer(String userName, String password, String[] serviceEndpoints) {
        this.userName = userName;
        this.password = password;
        this.serviceEndpoints = serviceEndpoints;
    }

    public ProxyEndpoint start() {
        this.proxyService = new WireMockServer(WireMockConfiguration
                .options()
                .dynamicPort()
                .extensions(new ResponseTransformer() {
                    @Override
                    public Response transform(Request request,
                            Response response,
                            FileSource fileSource,
                            Parameters parameters) {
                        String proxyAuthorization = request.getHeader("Proxy-Authorization");
                        if (proxyAuthorization == null) {
                            HttpHeader proxyAuthenticateHeader = new HttpHeader("Proxy-Authenticate", "Basic");
                            return new Response.Builder()
                                    .status(407)
                                    .headers(new HttpHeaders(proxyAuthenticateHeader))
                                    .build();
                        } else {
                            if (!proxyAuthorization.startsWith("Basic")) {
                                return new Response.Builder()
                                        .status(401)
                                        .build();
                            }
                            String encodedCred = proxyAuthorization.substring("Basic".length());
                            encodedCred = encodedCred.trim();
                            final Base64.Decoder decoder = Base64.getDecoder();
                            final byte[] decodedCred = decoder.decode(encodedCred);
                            if (new String(decodedCred).equals(userName + ":" + password)) {
                                return new Response.Builder()
                                        .status(200)
                                        .build();
                            } else {
                                return new Response.Builder()
                                        .status(401)
                                        .build();
                            }
                        }
                    }

                    @Override
                    public String getName() {
                        return "ProxyServer";
                    }
                })
                .disableRequestJournal());
        for (String endpoint : this.serviceEndpoints) {
            proxyService.stubFor(WireMock.any(WireMock.urlEqualTo(endpoint))
                    .willReturn(WireMock.aResponse()));
        }
        this.proxyService.start();
        return new ProxyEndpoint("localhost", this.proxyService.port());
    }

    public void shutdown() {
        if (this.proxyService != null && this.proxyService.isRunning()) {
            this.proxyService.shutdown();

        }
    }

    static class ProxyEndpoint {
        private final String host;
        private final int port;

        ProxyEndpoint(String host, int port) {
            this.host = host;
            this.port = port;
        }

        String getHost() {
            return this.host;
        }

        int getPort() {
            return this.port;
        }
    }
}
