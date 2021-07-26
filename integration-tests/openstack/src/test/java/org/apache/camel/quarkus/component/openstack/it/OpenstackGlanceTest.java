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
package org.apache.camel.quarkus.component.openstack.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import static io.restassured.RestAssured.post;

@QuarkusTest
@QuarkusTestResource(OpenStackTestResource.class)
class OpenstackGlanceTest {

    //@Test
    public void createShouldSucceed() {
        post("/openstack/glance/createShouldSucceed").then().statusCode(204);
    }

    //@Test
    public void uploadShouldSucceed() {
        post("/openstack/glance/uploadShouldSucceed").then().statusCode(204);
    }

    //@Test
    public void getShouldSucceed() {
        post("/openstack/glance/getShouldSucceed").then().statusCode(204);
    }

    //@Test
    public void getAllShouldSucceed() {
        post("/openstack/glance/getAllShouldSucceed").then().statusCode(204);
    }

    //@Test
    public void deleteShouldSucceed() {
        post("/openstack/glance/deleteShouldSucceed").then().statusCode(204);
    }
}
