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

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Objects;
import java.util.regex.Pattern;

import com.azure.core.http.HttpClient;
import com.azure.core.http.ProxyOptions;
import com.azure.core.util.Configuration;
import com.azure.core.util.CoreUtils;
import com.azure.core.util.logging.ClientLogger;
import io.vertx.core.Vertx;
import io.vertx.core.net.ProxyType;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

import static com.azure.core.util.Configuration.PROPERTY_AZURE_REQUEST_CONNECT_TIMEOUT;
import static com.azure.core.util.Configuration.PROPERTY_AZURE_REQUEST_READ_TIMEOUT;
import static com.azure.core.util.Configuration.PROPERTY_AZURE_REQUEST_WRITE_TIMEOUT;
import static com.azure.core.util.CoreUtils.getDefaultTimeoutFromEnvironment;

/**
 * Builds a {@link VertxHttpClient}.
 */
public class VertxHttpClientBuilder {
    private static final Pattern NON_PROXY_HOSTS_SPLIT = Pattern.compile("(?<!\\\\)\\|");
    private static final Pattern NON_PROXY_HOST_DESANITIZE = Pattern.compile("(\\?|\\\\|\\(|\\)|\\\\E|\\\\Q|\\.\\.)");
    private static final Pattern NON_PROXY_HOST_DOT_STAR = Pattern.compile("(\\.\\*)");
    private static final long DEFAULT_CONNECT_TIMEOUT;
    private static final long DEFAULT_WRITE_TIMEOUT;
    private static final long DEFAULT_READ_TIMEOUT;

    static {
        ClientLogger logger = new ClientLogger(VertxHttpClientBuilder.class);
        Configuration configuration = Configuration.getGlobalConfiguration();
        DEFAULT_CONNECT_TIMEOUT = getDefaultTimeoutFromEnvironment(configuration,
                PROPERTY_AZURE_REQUEST_CONNECT_TIMEOUT, Duration.ofSeconds(10), logger).toMillis();
        DEFAULT_WRITE_TIMEOUT = getDefaultTimeoutFromEnvironment(configuration, PROPERTY_AZURE_REQUEST_WRITE_TIMEOUT,
                Duration.ofSeconds(60), logger).toSeconds();
        DEFAULT_READ_TIMEOUT = getDefaultTimeoutFromEnvironment(configuration, PROPERTY_AZURE_REQUEST_READ_TIMEOUT,
                Duration.ofSeconds(60), logger).toSeconds();
    }

    private Duration readIdleTimeout;
    private Duration writeIdleTimeout;
    private Duration connectTimeout;
    private Duration idleTimeout = Duration.ofSeconds(60);
    private ProxyOptions proxyOptions;
    private Configuration configuration;
    private WebClientOptions webClientOptions;
    private final Vertx vertx;

    /**
     * Creates VertxAsyncHttpClientBuilder.
     *
     * @param vertx The {@link Vertx} instance to pass to the {@link WebClient}.
     */
    public VertxHttpClientBuilder(Vertx vertx) {
        Objects.requireNonNull(vertx, "vertx cannot be null");
        this.vertx = vertx;
    }

    /**
     * Sets the read idle timeout.
     *
     * The default read idle timeout is 60 seconds.
     *
     * @param  readIdleTimeout the read idle timeout
     * @return                 the updated VertxAsyncHttpClientBuilder object
     */
    public VertxHttpClientBuilder readIdleTimeout(Duration readIdleTimeout) {
        this.readIdleTimeout = readIdleTimeout;
        return this;
    }

    /**
     * Sets the write idle timeout.
     *
     * The default read idle timeout is 60 seconds.
     *
     * @param  writeIdleTimeout the write idle timeout
     * @return                  the updated VertxAsyncHttpClientBuilder object
     */
    public VertxHttpClientBuilder writeIdleTimeout(Duration writeIdleTimeout) {
        this.writeIdleTimeout = writeIdleTimeout;
        return this;
    }

    /**
     * Sets the connect timeout.
     *
     * The default connect timeout is 10 seconds.
     *
     * @param  connectTimeout the connection timeout
     * @return                the updated VertxAsyncHttpClientBuilder object
     */
    public VertxHttpClientBuilder connectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    /**
     * Sets the connection idle timeout.
     *
     * The default connect timeout is 60 seconds.
     *
     * @param  idleTimeout the connection idle timeout
     * @return             the updated VertxAsyncHttpClientBuilder object
     */
    public VertxHttpClientBuilder idleTimeout(Duration idleTimeout) {
        this.idleTimeout = idleTimeout;
        return this;
    }

    /**
     * Sets proxy configuration.
     *
     * @param  proxyOptions The proxy configuration to use.
     * @return              The updated VertxAsyncHttpClientBuilder object.
     */
    public VertxHttpClientBuilder proxy(ProxyOptions proxyOptions) {
        this.proxyOptions = proxyOptions;
        return this;
    }

    /**
     * Sets the configuration store that is used during construction of the HTTP client.
     * <p>
     * The default configuration store is a clone of the {@link Configuration#getGlobalConfiguration() global
     * configuration store}, use {@link Configuration#NONE} to bypass using configuration settings during construction.
     *
     * @param  configuration The configuration store.
     * @return               The updated VertxAsyncHttpClientBuilder object.
     */
    public VertxHttpClientBuilder configuration(Configuration configuration) {
        this.configuration = configuration;
        return this;
    }

    /**
     * Sets custom {@link WebClientOptions} for the constructed {@link WebClient}.
     *
     * @param  webClientOptions The options of the web client.
     * @return                  The updated VertxAsyncHttpClientBuilder object
     */
    public VertxHttpClientBuilder webClientOptions(WebClientOptions webClientOptions) {
        this.webClientOptions = webClientOptions;
        return this;
    }

    /**
     * Creates a new Vert.x {@link com.azure.core.http.HttpClient} instance on every call, using the
     * configuration set in the builder at the time of the build method call.
     *
     * @return A new Vert.x backed {@link com.azure.core.http.HttpClient} instance.
     */
    public HttpClient build() {
        if (this.webClientOptions == null) {
            this.webClientOptions = new WebClientOptions();
        }

        if (this.connectTimeout != null) {
            this.webClientOptions.setConnectTimeout((int) this.connectTimeout.toMillis());
        } else {
            this.webClientOptions.setConnectTimeout((int) DEFAULT_CONNECT_TIMEOUT);
        }

        if (this.readIdleTimeout != null) {
            this.webClientOptions.setReadIdleTimeout((int) this.readIdleTimeout.toSeconds());
        } else {
            this.webClientOptions.setReadIdleTimeout((int) DEFAULT_READ_TIMEOUT);
        }

        if (this.writeIdleTimeout != null) {
            this.webClientOptions.setWriteIdleTimeout((int) this.writeIdleTimeout.toSeconds());
        } else {
            this.webClientOptions.setWriteIdleTimeout((int) DEFAULT_WRITE_TIMEOUT);
        }

        this.webClientOptions.setIdleTimeout((int) this.idleTimeout.toSeconds());

        Configuration buildConfiguration = (configuration == null)
                ? Configuration.getGlobalConfiguration()
                : configuration;

        ProxyOptions buildProxyOptions = (this.proxyOptions == null && buildConfiguration != Configuration.NONE)
                ? ProxyOptions.fromConfiguration(buildConfiguration, true)
                : this.proxyOptions;

        if (buildProxyOptions != null) {
            io.vertx.core.net.ProxyOptions vertxProxyOptions = new io.vertx.core.net.ProxyOptions();
            InetSocketAddress proxyAddress = buildProxyOptions.getAddress();

            if (proxyAddress != null) {
                vertxProxyOptions.setHost(proxyAddress.getHostName());
                vertxProxyOptions.setPort(proxyAddress.getPort());
            }

            String proxyUsername = buildProxyOptions.getUsername();
            String proxyPassword = buildProxyOptions.getPassword();
            if (proxyUsername != null && proxyPassword != null) {
                vertxProxyOptions.setUsername(proxyUsername);
                vertxProxyOptions.setPassword(proxyPassword);
            }

            ProxyOptions.Type type = buildProxyOptions.getType();
            if (type != null) {
                try {
                    ProxyType proxyType = ProxyType.valueOf(type.name());
                    vertxProxyOptions.setType(proxyType);
                } catch (IllegalArgumentException e) {
                    throw new IllegalStateException("Unknown Vert.x proxy type: " + type.name(), e);
                }
            }

            String nonProxyHosts = buildProxyOptions.getNonProxyHosts();
            if (!CoreUtils.isNullOrEmpty(nonProxyHosts)) {
                for (String nonProxyHost : desanitizedNonProxyHosts(nonProxyHosts)) {
                    this.webClientOptions.addNonProxyHost(nonProxyHost);
                }
            }

            webClientOptions.setProxyOptions(vertxProxyOptions);
        }

        WebClient client = WebClient.create(this.vertx, this.webClientOptions);
        return new VertxHttpClient(client, this.webClientOptions);
    }

    /**
     * Reverses non proxy host string sanitization applied by {@link ProxyOptions}.
     *
     * This is necessary as Vert.x will apply its own sanitization logic.
     *
     * @param  nonProxyHosts The list of non proxy hosts
     * @return               String array of desanitized proxy host strings
     */
    private String[] desanitizedNonProxyHosts(String nonProxyHosts) {
        String desanitzedNonProxyHosts = NON_PROXY_HOST_DESANITIZE.matcher(nonProxyHosts)
                .replaceAll("");

        desanitzedNonProxyHosts = NON_PROXY_HOST_DOT_STAR.matcher(desanitzedNonProxyHosts)
                .replaceAll("*");

        return NON_PROXY_HOSTS_SPLIT.split(desanitzedNonProxyHosts);
    }
}
