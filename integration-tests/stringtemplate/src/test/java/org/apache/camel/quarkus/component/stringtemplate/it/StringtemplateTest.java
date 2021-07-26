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
package org.apache.camel.quarkus.component.stringtemplate.it;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.component.stringtemplate.StringTemplateConstants;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;

@QuarkusTest
class StringtemplateTest {

    //@Test
    public void testTemplateFromClasspath() {
        Map<String, Object> headers = new HashMap() {
            {
                put("name", "Sheldon");
                put("item", "Camel in Action");
            }
        };
        RestAssured.given()
                .queryParam("body", "PS: Next beer is on me!")
                .queryParam("template", "//template/letter.tm")
                .contentType(ContentType.JSON)
                .body(headers)
                .post("/stringtemplate/template")
                .then()
                .statusCode(201)
                .body(containsString(
                        "Dear Sheldon! Thanks for the order of Camel in Action. Regards Camel Riders Bookstore PS: Next beer is on me!"));
    }

    //@Test
    public void testVariableMap() {

        Map<String, Object> variableMap = new HashMap<>();
        Map<String, Object> headersMap = new HashMap<>();
        headersMap.put("name", "Willem");
        variableMap.put("headers", headersMap);
        variableMap.put("body", "Monday");
        variableMap.put("item", "1");
        Map<String, Object> headers = new HashMap() {
            {
                put("name", "Sheldon");
                put("item", "7");
                put(StringTemplateConstants.STRINGTEMPLATE_VARIABLE_MAP, variableMap);
            }
        };
        //`allowTemplateFromHeader` is need to be set to true because of https://issues.apache.org/jira/browse/CAMEL-15577
        RestAssured.given()
                .queryParam("body", "")
                .queryParam("template", "//template/template.tm")
                .queryParam("parameters", "allowTemplateFromHeader=true")
                .contentType(ContentType.JSON)
                .body(headers)
                .post("/stringtemplate/template")
                .then()
                .statusCode(201)
                .body(containsString(
                        "Dear Willem. You ordered item 1 on Monday."));
    }

    //@Test
    public void testWithBraceDelimiter() {
        testWithDelimiter("{", "}", "With brace delimiter ", "custom-delimiter-brace.tm");
    }

    //@Test
    public void testWithDollarDelimiter() {
        testWithDelimiter("$", "$", "With identical dollar delimiter ", "custom-delimiter-dollar.tm");
    }

    private void testWithDelimiter(String start, String stop, String text, String template) {
        RestAssured.given()
                .queryParam("body", "WORKS!")
                .queryParam("template", "//template/" + template)
                .queryParam("parameters", "delimiterStart=" + start + "&delimiterStop=" + stop)
                .contentType(ContentType.JSON)
                .body(Collections.emptyMap())
                .post("/stringtemplate/template")
                .then()
                .statusCode(201)
                .body(containsString(
                        text + "WORKS!\n"));
    }

    //@Test
    public void testContentCacheFalse() throws Exception {
        testContentCache(false);
    }

    //@Test
    public void testContentCacheTrue() throws Exception {
        testContentCache(true);
    }

    private void testContentCache(boolean useContentCache) throws Exception {
        File template = createFile("stringtemplate", "Hi <headers.name>");

        RestAssured.given()
                .queryParam("template", "file:" + template.getPath())
                .queryParam("parameters", "contentCache=" + useContentCache)
                .contentType(ContentType.JSON)
                .body(Collections.singletonMap("name", "Sheldon"))
                .post("/stringtemplate/template")
                .then()
                .statusCode(201)
                .body(equalTo("Hi Sheldon"));

        //override file
        Files.write(Paths.get(template.getPath()), "Bye <headers.name>".getBytes(StandardCharsets.UTF_8));

        RestAssured.given()
                .queryParam("template", "file:" + template.getPath())
                .queryParam("parameters", "contentCache=" + useContentCache)
                .contentType(ContentType.JSON)
                .body(Collections.singletonMap("name", "Sheldon"))
                .body(Collections.singletonMap("name", "Sheldon"))
                .post("/stringtemplate/template")
                .then()
                .statusCode(201)
                .body(equalTo(useContentCache ? "Hi Sheldon" : "Bye Sheldon"));
    }

    private File createFile(String fileName, String body) throws IOException {
        File tmpFile = File.createTempFile(fileName, ".tm");

        Files.write(tmpFile.toPath(), body.getBytes(StandardCharsets.UTF_8));

        tmpFile.deleteOnExit();
        return tmpFile;
    }

}
