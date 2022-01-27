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
package org.apache.camel.quarkus.component.knative.it;

import javax.ws.rs.core.MediaType;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.apache.camel.component.knative.http.KnativeHttpConsumerFactory;
import org.apache.camel.component.knative.http.KnativeHttpProducerFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestHTTPEndpoint(KnativeResource.class)
class KnativeTest {

    @Test
    public void inspect() {
        JsonPath p = RestAssured.given()
                .accept(MediaType.APPLICATION_JSON)
                .get("/inspect")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertEquals(KnativeHttpConsumerFactory.class.getName(), p.getString("consumer-factory"));
        assertEquals(KnativeHttpProducerFactory.class.getName(), p.getString("producer-factory"));
    }

}
