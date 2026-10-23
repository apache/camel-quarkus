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
package org.apache.camel.quarkus.component.dropbox.it;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Optional;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.http.StandardHttpRequestor;
import com.dropbox.core.v2.DbxClientV2;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public class DropboxProducers {

    /**
     * The Dropbox SDK always builds https URLs from bare host names, so it cannot be pointed at WireMock
     * via DbxHost. Instead, requests are redirected to WireMock at the HTTP layer.
     */
    @Produces
    @Singleton
    @Named("wireMockDropboxClient")
    DbxClientV2 wireMockDropboxClient(
            @ConfigProperty(name = "wiremock.url") Optional<String> wireMockUrl,
            @ConfigProperty(name = "DROPBOX_CLIENT_IDENTIFIER") String clientIdentifier,
            @ConfigProperty(name = "DROPBOX_ACCESS_TOKEN") String accessToken) {
        DbxRequestConfig config = DbxRequestConfig.newBuilder(clientIdentifier)
                .withHttpRequestor(new WireMockHttpRequestor(wireMockUrl.orElseThrow()))
                .build();
        return new DbxClientV2(config, accessToken);
    }

    static final class WireMockHttpRequestor extends StandardHttpRequestor {
        private final String wireMockUrl;

        WireMockHttpRequestor(String wireMockUrl) {
            super(Config.DEFAULT_INSTANCE);
            this.wireMockUrl = wireMockUrl;
        }

        @Override
        protected HttpURLConnection prepRequest(String url, Iterable<Header> headers, boolean streaming) throws IOException {
            URI uri = URI.create(url);
            String query = uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "";
            return super.prepRequest(wireMockUrl + uri.getRawPath() + query, headers, streaming);
        }
    }
}
