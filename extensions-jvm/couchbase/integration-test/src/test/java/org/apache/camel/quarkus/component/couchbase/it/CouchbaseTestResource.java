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
package org.apache.camel.quarkus.component.couchbase.it;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.testcontainers.couchbase.CouchbaseContainer;
import org.testcontainers.utility.DockerImageName;

public class CouchbaseTestResource implements QuarkusTestResourceLifecycleManager {
    private final static DockerImageName COUCHBASE_IMAGE = DockerImageName.parse("couchbase/server:6.5.1");
    public static final int KV_PORT = 11210;
    public static final int MANAGEMENT_PORT = 8091;
    public static final int VIEW_PORT = 8092;
    public static final int QUERY_PORT = 8093;
    public static final int SEARCH_PORT = 8094;
    protected String bucketName = "testBucket";

    private CustomCouchbaseContainer container;

    private class CustomCouchbaseContainer extends CouchbaseContainer {
        public CustomCouchbaseContainer() {
            super(COUCHBASE_IMAGE);

            addFixedExposedPort(KV_PORT, KV_PORT);
            addFixedExposedPort(MANAGEMENT_PORT, MANAGEMENT_PORT);
            addFixedExposedPort(VIEW_PORT, VIEW_PORT);
            addFixedExposedPort(QUERY_PORT, QUERY_PORT);
            addFixedExposedPort(SEARCH_PORT, SEARCH_PORT);
        }
    }

    @Override
    public Map<String, String> start() {
        container = new CustomCouchbaseContainer();

        container.start();

        return CollectionHelper.mapOf("couchbase.connection.uri", getConnectionUri(),
                "couchbase.bucket.name", bucketName,
                "couchbase.connection.string", container.getConnectionString(),
                "username", this.getUsername(),
                "password", this.getPassword());
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }

    public String getConnectionUri() {
        return String.format("couchbase:http://%s:%d?bucket=%s&username=%s&password=%s", getHostname(),
                getPort(), bucketName, getUsername(), getPassword());
    }

    public String getUsername() {
        return container.getUsername();
    }

    public String getPassword() {
        return container.getPassword();
    }

    public String getHostname() {
        return container.getHost();
    }

    public int getPort() {
        return container.getBootstrapHttpDirectPort();
    }
}
