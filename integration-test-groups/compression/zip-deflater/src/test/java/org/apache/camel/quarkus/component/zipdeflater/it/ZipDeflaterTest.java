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
package org.apache.camel.quarkus.component.zipdeflater.it;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@QuarkusTest
class ZipDeflaterTest {
    final byte[] UNCOMPRESSED = "Hello World Zip Deflater!".getBytes(StandardCharsets.UTF_8);

    private static Stream<String> listZipDataFormatsToBeTested() {
        return Stream.of("zip-deflater", "gzip-deflater");
    }

    @ParameterizedTest
    @MethodSource("listZipDataFormatsToBeTested")
    public void compressThenUncompressShouldSucceed(String format) throws Exception {

        final byte[] compressed = RestAssured.given()
                .contentType(ContentType.BINARY)
                .body(UNCOMPRESSED)
                .post("/zip-deflater/compress/" + format) //
                .then().extract().body().asByteArray();

        Assertions.assertNotNull(compressed);

        final byte[] uncompressed = RestAssured.given()
                .contentType(ContentType.BINARY)
                .body(compressed)
                .post("/zip-deflater/uncompress/" + format) //
                .then().extract().body().asByteArray();

        Assertions.assertArrayEquals(UNCOMPRESSED, uncompressed);
    }
}
