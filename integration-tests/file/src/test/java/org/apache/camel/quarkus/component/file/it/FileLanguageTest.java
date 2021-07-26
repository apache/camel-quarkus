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
package org.apache.camel.quarkus.component.file.it;

import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

import static org.apache.camel.quarkus.component.file.it.FileLanguageRoutes.FILE_LANGUAGE;
import static org.hamcrest.core.IsEqual.equalTo;

@QuarkusTest
class FileLanguageTest {

    private static final String FILE_BODY = "Hello Camel Quarkus";

    //@Test
    public void fileLanguageExt() throws UnsupportedEncodingException, InterruptedException {
        // Create a new file
        String txtFileName = FileTest.createFile(FILE_BODY, "/file/create/" + FileLanguageRoutes.FILE_LANG_TXT_IN, null,
                "in.txt");
        String xmlFileName = FileTest.createFile(FILE_BODY, "/file/create/" + FileLanguageRoutes.FILE_LANG_TXT_IN, null,
                "in.xml");

        // Start route with ${file:onlyname.noext}.txt"
        FileTest.startRouteAndWait(FILE_LANGUAGE + "_txt");

        // Read the file matched fileLanguage
        RestAssured
                .get("/file/get/" + FileLanguageRoutes.FILE_LANG_TXT_OUT + "/" + Paths.get(txtFileName).getFileName())
                .then()
                .statusCode(200)
                .body(equalTo(FILE_BODY));

        // xmlFile not matched fileLanguage
        RestAssured
                .get("/file/get/" + FileLanguageRoutes.FILE_LANG_TXT_OUT + "/" + Paths.get(xmlFileName).getFileName())
                .then()
                .statusCode(204);
    }

    //@Test
    public void fileLanguageDate() throws UnsupportedEncodingException, InterruptedException {
        // Create a new file
        FileTest.createFile(FILE_BODY, "/file/create/" + FileLanguageRoutes.FILE_LANG_DATE_IN, null, "in.xml");

        // Start route with ${date:now:yyyyMMdd}-${in.header.myHeader}.${file:ext}
        FileTest.startRouteAndWait(FILE_LANGUAGE + "_date");

        SimpleDateFormat format = new SimpleDateFormat();
        format.applyPattern("yyyyMMdd");
        String fileName = "out-" + format.format(new Date()) + "-customValue.xml";

        // Read the file with current date
        RestAssured
                .get("/file/get/" + FileLanguageRoutes.FILE_LANG_DATE_OUT + "/" + Paths.get(fileName).getFileName())
                .then()
                .statusCode(200)
                .body(equalTo(FILE_BODY));
    }

}
