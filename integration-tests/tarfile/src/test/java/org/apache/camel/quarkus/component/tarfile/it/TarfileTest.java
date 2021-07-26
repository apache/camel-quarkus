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
package org.apache.camel.quarkus.component.tarfile.it;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import org.apache.camel.util.IOHelper;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.Assertions;

@QuarkusTest
class TarfileTest {

    //@Test
    public void test() throws Exception {
        final String encoding = "utf-8";

        byte[] body;

        ExtractableResponse response = RestAssured.given() //
                .contentType(ContentType.TEXT + "; charset=" + encoding).body("Hello World").post("/tarfile/post") //
                .then().extract();

        body = response.body().asByteArray();
        Assertions.assertNotNull(body);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ByteArrayInputStream bis = new ByteArrayInputStream(body);
        TarArchiveInputStream tis = (TarArchiveInputStream) new ArchiveStreamFactory()
                .createArchiveInputStream(ArchiveStreamFactory.TAR, bis);

        TarArchiveEntry entry = tis.getNextTarEntry();
        if (entry != null) {
            IOHelper.copy(tis, bos);
        }

        String str = bos.toString(encoding);
        Assertions.assertEquals("Hello World", str);
    }

}
