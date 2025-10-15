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
package org.apache.camel.quarkus.transformer;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.impl.engine.DefaultTransformerRegistry;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class TransformerTest {

    @Test
    void testTransformerRegistryImpl() {
        RestAssured.get("/transformer/registry")
                .then()
                .body(is(DefaultTransformerRegistry.class.getName()));
    }

    /**
     * Verify transformer using @DataTypeTransformer annotation is in registry
     */
    @Test
    void testTransformerRegistryContainsDataTypeTransformerAnnotated() {
        RestAssured.get("/transformer/registry/values")
                .then()
                .body(containsString("UppercaseTransformer"));
    }

    /**
     * Verify named transformers are in registry
     */
    @Test
    void testTransformerRegistryContainsNamedTransformers() {
        RestAssured.get("/transformer/registry/values")
                .then()
                .body(containsString("TrimTransformer"))
                .body(containsString("UppercaseTransformer"));
    }

    @Test
    void testTransformBeanToString() {
        RestAssured.given()
                .body("To String")
                .when()
                .post("/transformer/toString")
                .then()
                .body(is("Transformed To String"));
    }

    @Test
    void testTransformBeanToBytes() {
        RestAssured.given()
                .body("To Bytes")
                .when()
                .post("/transformer/toBytes")
                .then()
                .body(is("Transformed To Bytes"));
    }

    /**
     * Test DataFormat Transformer
     */
    @Test
    void testTransformBeanToJson() {
        RestAssured.given()
                .body("Transformed To Json")
                .when()
                .post("/transformer/toJson")
                .then()
                .body(is("{\"message\":\"Transformed To Json\"}"));
    }

    /**
     * Test @DataTypeTransformer annotation
     */
    @Test
    void testTransformStringToUppercase() {
        RestAssured.given()
                .body("To Uppercase")
                .when()
                .post("/transformer/toUppercase")
                .then()
                .body(is("TRANSFORMED TO UPPERCASE"));
    }

    /**
     * Test Named @DataTypeTransformer annotation
     */
    @Test
    void testTransformStringToTrimmed() {
        RestAssured.given()
                .body("  To Trimmed  ")
                .when()
                .post("/transformer/toTrimmed")
                .then()
                .body(is("Transformed To Trimmed"));
    }

    /**
     * Test Custom Transformer
     */
    @Test
    void testTransformStringToLowercase() {
        RestAssured.given()
                .body("To Lowercase")
                .when()
                .post("/transformer/toLowercase")
                .then()
                .body(is("transformed to lowercase"));
    }

    /**
     * Test Endpoint Transformer
     */
    @Test
    void testTransformStringToReversed() {
        RestAssured.given()
                .body("To Reversed")
                .when()
                .post("/transformer/toReversed")
                .then()
                .body(is("Transformed desreveR oT"));
    }

}
