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
package org.apache.camel.quarkus.test.support.aws2;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Supplier;

import org.jboss.logging.Logger;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.regions.Region;

/**
 * A context passed to {@link Aws2TestEnvCustomizer#customize(Aws2TestEnvContext)}.
 */
public class Aws2TestEnvContext {
    private static final Logger LOG = Logger.getLogger(Aws2TestEnvContext.class);
    private final ArrayList<AutoCloseable> closeables = new ArrayList<>();
    private final Map<Service, ? extends SdkClient> clients = new EnumMap<>(Service.class);
    private final Map<String, String> properties = new LinkedHashMap<>();
    private final Optional<LocalStackContainer> localstack;
    private final Aws2TestConfig aws2TestConfig;

    public Aws2TestEnvContext(Aws2TestConfig aws2TestConfig, Optional<LocalStackContainer> localstack,
            Service[] exportCredentialsServices) {
        this.localstack = localstack;
        this.aws2TestConfig = aws2TestConfig;

        for (Service service : exportCredentialsServices) {
            if (aws2TestConfig.isAwsQuarkusClientTest()) {
                String prefix = String.format("quarkus.%s", service.getName());
                properties.put(prefix + ".aws.credentials.static-provider.access-key-id", aws2TestConfig.getAccessKey());
                properties.put(prefix + ".aws.credentials.static-provider.secret-access-key",
                        aws2TestConfig.getAccessSecret());
                properties.put(prefix + ".aws.region", aws2TestConfig.getRegion());
                properties.put(prefix + ".aws.credentials.type", "static");
                localstack.ifPresent(ls -> {
                    properties.put(prefix + ".endpoint-override", ls.getEndpointOverride(service).toString());
                });
            } else {
                String prefix = String.format("camel.component.aws2-%s", camelServiceAcronym(service));
                properties.put(prefix + ".access-key", aws2TestConfig.getAccessSecret());
                properties.put(prefix + ".secret-key", aws2TestConfig.getAccessSecret());
                properties.put(prefix + ".region", aws2TestConfig.getRegion());
                properties.put(prefix + ".override-endpoint", "true");
                localstack.ifPresent(ls -> {
                    properties.put(prefix + ".uri-endpoint-override", ls.getEndpointOverride(service).toString());
                });
            }
        }
    }

    /**
     * Add a key-value pair to the system properties seen by AWS 2 tests
     *
     * @param  key
     * @param  value
     * @return       this {@link Aws2TestEnvContext}
     */
    public Aws2TestEnvContext property(String key, String value) {
        properties.put(key, value);
        return this;
    }

    /**
     * Add an {@link AutoCloseable} to be closed after running AWS 2 tests
     *
     * @param  closeable the {@link AutoCloseable} to add
     * @return           this {@link Aws2TestEnvContext}
     */
    public Aws2TestEnvContext closeable(AutoCloseable closeable) {
        closeables.add(closeable);
        return this;
    }

    /**
     * @return a read-only view of {@link #properties}
     */
    public Map<String, String> getProperies() {
        return Collections.unmodifiableMap(properties);
    }

    /**
     * Close all {@link AutoCloseable}s registered via {@link #closeable(AutoCloseable)}
     */
    public void close() {
        ListIterator<AutoCloseable> it = closeables.listIterator(closeables.size());
        while (it.hasPrevious()) {
            AutoCloseable c = it.previous();
            if (c instanceof SdkClient) {
                clients.entrySet().stream().filter(en -> c == en.getValue()).map(Entry::getKey).findFirst()
                        .map(clients::remove);
            }
            try {
                c.close();
            } catch (Exception e) {
                LOG.warnf(e, "Could not close %s", c);
            }
        }

        if (localstack.isPresent()) {
            localstack.get().stop();
        }
    }

    /**
     * Create a new AWS 2 client and register it for closing after running AWS 2 tests.
     *
     * @param  <B>
     * @param  <C>
     * @param  service
     * @param  builderSupplier
     * @return                 a new client
     */
    public <B extends AwsClientBuilder<B, C>, C extends SdkClient> C client(
            Service service,
            Supplier<B> builderSupplier) {
        B builder = builderSupplier.get()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                        aws2TestConfig.getAccessKey(), aws2TestConfig.getAccessSecret())));
        builder.region(Region.of(aws2TestConfig.getRegion()));

        if (localstack.isPresent()) {
            builder
                    .endpointOverride(localstack.get().getEndpointOverride(service))
                    .region(Region.of(aws2TestConfig.getRegion()));
        } else if (service == Service.IAM) {
            /* Avoid UnknownHostException: iam.eu-central-1.amazonaws.com */
            builder.endpointOverride(URI.create("https://iam.amazonaws.com"));
            builder.region(Region.of("us-east-1"));
        }

        final C client = builder.build();
        closeables.add(client);
        return client;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    SdkClient client(Service service, Class<?> clientType) {
        SdkClient result = clients.get(service);
        if (result != null) {
            return result;
        }
        return client(service, () -> {
            try {
                return (AwsClientBuilder) clientType.getDeclaredMethod("builder").invoke(null);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
                    | SecurityException e) {
                throw new RuntimeException("Could not call " + clientType.getName() + ".builder()", e);
            }
        });
    }

    private static String camelServiceAcronym(Service service) {
        switch (service) {
        case DYNAMODB:
            return "ddb";
        case DYNAMODB_STREAMS:
            return "ddbstream";
        case FIREHOSE:
            return "kinesis-firehose";
        case CLOUDWATCH:
            return "cw";
        default:
            return service.name().toLowerCase(Locale.ROOT);
        }
    }
}
