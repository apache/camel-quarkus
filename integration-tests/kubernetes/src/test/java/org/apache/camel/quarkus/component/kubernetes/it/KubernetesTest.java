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
package org.apache.camel.quarkus.component.kubernetes.it;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesServerTestResource;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

@Disabled("https://github.com/apache/camel-quarkus/issues/4095")
@QuarkusTest
@QuarkusTestResource(KubernetesServerTestResource.class)
public class KubernetesTest {

    @KubernetesTestServer
    private KubernetesServer mockServer;

    @Test
    public void testKubernetesComponent() {
        Container container = new Container();
        container.setImage("busybox:latest");
        container.setName("camel-pod");

        Pod pod = new PodBuilder()
                .withNewMetadata()
                .withName("camel-pod")
                .withNamespace("test")
                .and()
                .withNewSpec()
                .withContainers(container)
                .and()
                .build();

        mockServer.expect()
                .post()
                .withPath("/api/v1/namespaces/test/pods")
                .andReturn(201, pod)
                .once();

        mockServer.expect()
                .get()
                .withPath("/api/v1/namespaces/test/pods/camel-pod")
                .andReturn(200, pod)
                .always();

        mockServer.expect()
                .delete()
                .withPath("/api/v1/namespaces/test/pods/camel-pod")
                .andReturn(200, "{}")
                .once();

        RestAssured.when()
                .post("/kubernetes/pod/test/camel-pod")
                .then()
                .statusCode(201);

        RestAssured.when()
                .get("/kubernetes/pod/test/camel-pod")
                .then()
                .statusCode(200)
                .body(is("camel-pod"));

        RestAssured.when()
                .delete("/kubernetes/pod/test/camel-pod")
                .then()
                .statusCode(204);
    }
}
