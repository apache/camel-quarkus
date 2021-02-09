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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

import org.apache.camel.quarkus.test.mock.backend.MockBackendUtils;
import org.apache.camel.quarkus.testcontainers.ContainerResourceLifecycleManager;
import org.jboss.logging.Logger;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.utility.DockerImageName;

public abstract class Aws2TestResource implements ContainerResourceLifecycleManager {

    private static final Logger LOG = Logger.getLogger(Aws2TestResource.class);

    protected final ArrayList<AutoCloseable> closeables = new ArrayList<>();

    protected final Service[] services;

    protected LocalStackContainer localstack;

    protected boolean usingMockBackend;

    protected String accessKey;
    protected String secretKey;
    protected String region;

    public Aws2TestResource(Service first, Service... other) {
        final Service[] s = new Service[other.length + 1];
        s[0] = first;
        System.arraycopy(other, 0, s, 1, other.length);
        this.services = s;
    }

    @SuppressWarnings("resource")
    @Override
    public Map<String, String> start() {
        final String realKey = System.getenv("AWS_ACCESS_KEY");
        final String realSecret = System.getenv("AWS_SECRET_KEY");
        final String realRegion = System.getenv("AWS_REGION");
        final boolean realCredentialsProvided = realKey != null && realSecret != null && realRegion != null;
        final boolean startMockBackend = MockBackendUtils.startMockBackend(false);
        final Map<String, String> result = new LinkedHashMap<>();
        usingMockBackend = startMockBackend && !realCredentialsProvided;
        if (usingMockBackend) {
            MockBackendUtils.logMockBackendUsed();
            this.localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:0.12.6"))
                    .withServices(services);
            closeables.add(localstack);
            localstack.start();

            this.accessKey = localstack.getAccessKey();
            this.secretKey = localstack.getSecretKey();
            this.region = localstack.getRegion();

            setMockProperties(result);

        } else {
            if (!startMockBackend && !realCredentialsProvided) {
                throw new IllegalStateException(
                        "Set AWS_ACCESS_KEY, AWS_SECRET_KEY and AWS_REGION env vars if you set CAMEL_QUARKUS_START_MOCK_BACKEND=false");
            }
            MockBackendUtils.logRealBackendUsed();
            this.accessKey = realKey;
            this.secretKey = realSecret;
            this.region = realRegion;
        }

        return result;
    }

    protected void setMockProperties(final Map<String, String> result) {
        for (Service service : services) {
            String s = serviceKey(service);
            result.put("camel.component.aws2-" + s + ".access-key", accessKey);
            result.put("camel.component.aws2-" + s + ".secret-key", secretKey);
            result.put("camel.component.aws2-" + s + ".region", region);

            switch (service) {
            case SQS:
            case SNS:
                // TODO https://github.com/apache/camel-quarkus/issues/2216
                break;
            default:
                result.put("camel.component.aws2-" + s + ".override-endpoint", "true");
                result.put("camel.component.aws2-" + s + ".uri-endpoint-override",
                        localstack.getEndpointOverride(service).toString());
                break;
            }
        }
    }

    protected String serviceKey(Service service) {
        return service.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public void stop() {
        ListIterator<AutoCloseable> it = closeables.listIterator(closeables.size());
        while (it.hasPrevious()) {
            AutoCloseable c = it.previous();
            try {
                c.close();
            } catch (Exception e) {
                LOG.warnf(e, "Could not close %s", c);
            }
        }
    }
}
