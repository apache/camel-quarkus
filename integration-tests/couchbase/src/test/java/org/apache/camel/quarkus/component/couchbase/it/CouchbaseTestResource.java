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

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.manager.bucket.BucketSettings;
import com.couchbase.client.java.manager.bucket.BucketType;
import com.couchbase.client.java.manager.view.DesignDocument;
import com.couchbase.client.java.manager.view.View;
import com.couchbase.client.java.view.DesignDocumentNamespace;
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
    protected Cluster cluster;

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

        initBucket();

        return CollectionHelper.mapOf("couchbase.connection.uri", getConnectionUri(),
                "couchbase.bucket.name", bucketName,
                "quarkus.couchbase.connection-string", String.format("couchbase:http://%s:%d", getHostname(),
                        getPort()),
                "quarkus.couchbase.username", getUsername(),
                "quarkus.couchbase.password", getPassword());
    }

    @Override
    public void stop() {
        if (cluster != null) {
            cluster.buckets().dropBucket(bucketName);
            cluster.disconnect();
        }
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

    private void initBucket() {
        cluster = Cluster.connect(container.getConnectionString(), this.getUsername(), this.getPassword());

        cluster.buckets().createBucket(
                BucketSettings.create(bucketName).bucketType(BucketType.COUCHBASE).flushEnabled(true));

        cluster.bucket(bucketName);
        DesignDocument designDoc = new DesignDocument(
                bucketName,
                Collections.singletonMap(bucketName, new View("function (doc, meta) {  emit(meta.id, doc);}")));
        cluster.bucket(bucketName).viewIndexes().upsertDesignDocument(designDoc, DesignDocumentNamespace.PRODUCTION);
        // wait for cluster
        cluster.bucket(bucketName).waitUntilReady(Duration.ofSeconds(30));

        // insert some documents
        for (int i = 0; i < 3; i++) {
            cluster.bucket(bucketName).defaultCollection().upsert("DocumentID_" + i, "hello" + i);
        }

        // wait for cluster
        cluster.bucket(bucketName).waitUntilReady(Duration.ofSeconds(30));
    }
}
