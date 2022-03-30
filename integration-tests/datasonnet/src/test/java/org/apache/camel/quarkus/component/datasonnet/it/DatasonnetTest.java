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
package org.apache.camel.quarkus.component.datasonnet.it;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DatasonnetTest {

    @Test
    public void testTransform() throws Exception {
        final String msg = loadResourceAsString("simpleMapping_payload.json");
        RestAssured.given() //
                .contentType(ContentType.JSON)
                .body(msg)
                .post("/datasonnet/basicTransform") //
                .then()
                .statusCode(201);

        //        Assertions.fail("Add some assertions to " + getClass().getName());
        //
        //        RestAssured.get("/datasonnet/get")
        //                .then()
        //                .statusCode(200);
    }

    private String loadResourceAsString(String name) throws Exception {
        //        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
        //        return IOUtils.toString(is, Charset.defaultCharset());
        Path path = Paths.get(Thread.currentThread().getContextClassLoader()
                .getResource(name).toURI());

        Stream<String> lines = Files.lines(path);
        String data = lines.collect(Collectors.joining("\n"));
        lines.close();

        return data;
    }

}
