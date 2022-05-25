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
package org.apache.camel.quarkus.component.aws.secrets.manager.it;

import java.util.Collections;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.component.aws.secretsmanager.SecretsManagerConstants;
import org.apache.camel.component.aws.secretsmanager.SecretsManagerOperations;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestResource;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(Aws2TestResource.class)
class AwsSecretsManagerTest {

    @Test
    public void testOperations() {
        final String secretToCreate = "changeit";
        final String secret2ToCreate = "changeit2";
        final String secretToUpdate = "changeit123";
        final String nameToCreate = "TestSecret";
        final String name2ToCreate = "TestSecret2";

        String createdArn = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Collections.singletonMap(SecretsManagerConstants.SECRET_NAME, nameToCreate))
                .queryParam("body", secretToCreate)
                .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.createSecret)
                .then()
                .statusCode(201)
                .extract().asString();

        assertNotNull(createdArn);

        String createdArn2 = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Collections.singletonMap(SecretsManagerConstants.SECRET_NAME, name2ToCreate))
                .queryParam("body", secret2ToCreate)
                .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.createSecret)
                .then()
                .statusCode(201)
                .extract().asString();

        assertNotNull(createdArn);

        Map secrets = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Collections.emptyMap())
                .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.listSecrets)
                .then()
                .statusCode(201)
                .extract().as(Map.class);

        assertEquals(2, secrets.size());
        assertTrue(secrets.containsKey(createdArn));
        assertTrue(secrets.containsKey(createdArn2));
        //none of them is deleted
        assertFalse(secrets.containsValue(true));

        String secret = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Collections.singletonMap(SecretsManagerConstants.SECRET_ID, createdArn))
                .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.getSecret)
                .then()
                .statusCode(201)
                .extract().asString();

        assertEquals(secretToCreate, secret);

        Map description = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Collections.singletonMap(SecretsManagerConstants.SECRET_ID, createdArn))
                .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.describeSecret)
                .then()
                .statusCode(201)
                .extract().as(Map.class);

        assertEquals(2, description.size());
        assertEquals(true, description.get("sdkHttpSuccessful"));
        assertEquals(nameToCreate, description.get("name"));

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Collections.singletonMap(SecretsManagerConstants.SECRET_ID, createdArn2))
                .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.deleteSecret)
                .then()
                .statusCode(201)
                .body(is("true"));

        secrets = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Collections.emptyMap())
                .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.listSecrets)
                .then()
                .statusCode(201)
                .extract().as(Map.class);

        assertEquals(2, secrets.size());
        assertFalse((Boolean) secrets.get(createdArn));
        assertTrue((Boolean) secrets.get(createdArn2));

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Collections.singletonMap(SecretsManagerConstants.SECRET_ID, createdArn))
                .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.rotateSecret)
                .then()
                .statusCode(201)
                .body(is("true"));

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Collections.singletonMap(SecretsManagerConstants.SECRET_ID, createdArn))
                .queryParam("body", secretToUpdate)
                .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.updateSecret)
                .then()
                .statusCode(201)
                .body(is("true"));

        String updatedSecret = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Collections.singletonMap(SecretsManagerConstants.SECRET_ID, createdArn))
                .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.getSecret)
                .then()
                .statusCode(201)
                .extract().asString();

        assertEquals(secretToUpdate, updatedSecret);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Collections.singletonMap(SecretsManagerConstants.SECRET_ID, createdArn2))
                .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.restoreSecret)
                .then()
                .statusCode(201)
                .body(is("true"));

        secrets = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Collections.emptyMap())
                .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.listSecrets)
                .then()
                .statusCode(201)
                .extract().as(Map.class);

        assertEquals(2, secrets.size());
        assertTrue(secrets.containsKey(createdArn));
        assertTrue(secrets.containsKey(createdArn2));
        //none of them is deleted, because it was restored
        assertFalse(secrets.containsValue(true));

        //        operation replicateSecretToRegions fails on local stack with 500
        //        RestAssured.given()
        //                .contentType(ContentType.JSON)
        //                .body(CollectionHelper.mapOf(SecretsManagerConstants.SECRET_ID, createdArn2,
        //                        SecretsManagerConstants.SECRET_REPLICATION_REGIONS, "us-east-1"))
        //                .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.replicateSecretToRegions)
        //                .then()
        //                .statusCode(201)
        //                .body(is("true"));
    }

}
