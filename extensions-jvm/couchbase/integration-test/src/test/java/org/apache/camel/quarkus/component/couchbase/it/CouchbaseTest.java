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

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.manager.bucket.BucketSettings;
import com.couchbase.client.java.manager.bucket.BucketType;
import com.couchbase.client.java.manager.view.DesignDocument;
import com.couchbase.client.java.manager.view.View;
import com.couchbase.client.java.view.DesignDocumentNamespace;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

@QuarkusTest
@TestHTTPEndpoint(CouchbaseResource.class)
@QuarkusTestResource(CouchbaseTestResource.class)
public class CouchbaseTest {

    protected static String bucketName;
    protected static Cluster cluster;

    @BeforeAll
    public static void setUpCouchbase() {
        bucketName = ConfigProvider.getConfig().getValue("couchbase.bucket.name", String.class);

        String conenctionString = ConfigProvider.getConfig().getValue("couchbase.connection.string", String.class);
        String username = ConfigProvider.getConfig().getValue("username", String.class);
        String password = ConfigProvider.getConfig().getValue("password", String.class);
        cluster = Cluster.connect(conenctionString, username, password);

        cluster.buckets().createBucket(
                BucketSettings.create(bucketName).bucketType(BucketType.COUCHBASE).flushEnabled(true));

        Bucket bucket = cluster.bucket(bucketName);
        DesignDocument designDoc = new DesignDocument(
                bucketName,
                Collections.singletonMap(bucketName, new View("function (doc, meta) {  emit(meta.id, doc);}")));
        cluster.bucket(bucketName).viewIndexes().upsertDesignDocument(designDoc, DesignDocumentNamespace.PRODUCTION);
    }

    @Test
    void testUpdate() {
        clusterReady();
        // updating the document
        given()
                .contentType(ContentType.TEXT)
                .body("updating hello2")
                .when()
                .put("/id/DocumentID_2")
                .then()
                .statusCode(200)
                .body(equalTo("true"));

        clusterReady();
        // check the result of update
        given()
                .when()
                .get("/DocumentID_2")
                .then()
                .statusCode(200)
                .body(equalTo("updating hello2"));

        clusterReady();

        // poll the first document
        given()
                .when()
                .get("/poll")
                .then()
                .statusCode(200)
                .body(equalTo("updating hello2"));

        clusterReady();
        // deleting the document
        given()
                .when()
                .delete("DocumentID_2")
                .then()
                .statusCode(200);
    }

    /**
     * waits until cluster is ready
     */
    private void clusterReady() {
        cluster.bucket(bucketName).waitUntilReady(Duration.ofSeconds(120));
    }

    @AfterAll
    public static void tearDownCouchbase() {
        cluster.buckets().dropBucket(bucketName);
        cluster.disconnect();
    }
}
