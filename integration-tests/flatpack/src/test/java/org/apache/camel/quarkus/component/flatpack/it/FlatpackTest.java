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
package org.apache.camel.quarkus.component.flatpack.it;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class FlatpackTest {

    @Test
    public void delimitedUnmarshalShouldSucceed() throws IOException {
        String data = IOUtils.toString(getClass().getResourceAsStream("/INVENTORY-CommaDelimitedWithQualifier.txt"),
                StandardCharsets.UTF_8);

        given().body(data).when().get("/flatpack/delimited-unmarshal").then().statusCode(200).body(is("4-SOME VALVE"));
    }

    @Test
    public void delimitedMarshalShouldSucceed() {
        List<Map<String, String>> data = new ArrayList<>();
        Map<String, String> firstRow = new LinkedHashMap<>();
        firstRow.put("ITEM_DESC", "SOME VALVE");
        firstRow.put("IN_STOCK", "2");
        firstRow.put("PRICE", "5.00");
        firstRow.put("LAST_RECV_DT", "20050101");
        data.add(firstRow);

        Map<String, String> secondRow = new LinkedHashMap<>();
        secondRow.put("ITEM_DESC", "AN ENGINE");
        secondRow.put("IN_STOCK", "100");
        secondRow.put("PRICE", "1000.00");
        secondRow.put("LAST_RECV_DT", "20040601");
        data.add(secondRow);

        String expected = "ITEM_DESC,IN_STOCK,PRICE,LAST_RECV_DT" + System.lineSeparator() + "AN ENGINE,100,1000.00,20040601"
                + System.lineSeparator();
        given().contentType(ContentType.JSON).body(data).when().get("/flatpack/delimited-marshal").then().statusCode(200)
                .body(is(expected));
    }

    @Test
    public void fixedLengthUnmarshalShouldSucceed() throws IOException {
        String data = IOUtils.toString(getClass().getResourceAsStream("/PEOPLE-FixedLength.txt"), StandardCharsets.UTF_8);
        given().body(data).when().get("/flatpack/fixed-length-unmarshal").then().statusCode(200).body(is("4-JOHN"));
    }

    @Test
    public void fixedLengthMarshalShouldSucceed() {
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("FIRSTNAME", "JOHN");
        row.put("LASTNAME", "DOE");
        row.put("ADDRESS", "1234 CIRCLE CT");
        row.put("CITY", "ELYRIA");
        row.put("STATE", "OH");
        row.put("ZIP", "44035");
        data.add(row);

        given().contentType(ContentType.JSON).body(data).when().get("/flatpack/fixed-length-marshal").then().statusCode(200)
                .body(startsWith("JOHN                               DOE"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void delimitedShouldSucceed() throws IOException {
        String data = IOUtils.toString(getClass().getResourceAsStream("/INVENTORY-CommaDelimitedWithQualifier.txt"),
                StandardCharsets.UTF_8);
        Map<String, String>[] rows = given().body(data).when().get("/flatpack/delimited").then().statusCode(200).extract()
                .as(Map[].class);
        assertNotNull(rows);
        assertEquals(4, rows.length);
        assertNotNull(rows[0]);
        assertEquals("SOME VALVE", rows[0].get("ITEM_DESC"));
        assertNotNull(rows[1]);
        assertEquals("AN ENGINE", rows[1].get("ITEM_DESC"));
        assertNotNull(rows[2]);
        assertEquals("A BELT", rows[2].get("ITEM_DESC"));
        assertNotNull(rows[3]);
        assertEquals("A BOLT", rows[3].get("ITEM_DESC"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void fixedShouldSucceed() throws IOException {
        String data = IOUtils.toString(getClass().getResourceAsStream("/PEOPLE-FixedLength.txt"), StandardCharsets.UTF_8);
        Map<String, String>[] rows = given().body(data).when().get("/flatpack/fixed").then().statusCode(200).extract()
                .as(Map[].class);
        assertNotNull(rows);
        assertEquals(4, rows.length);
        assertNotNull(rows[0]);
        assertEquals("JOHN", rows[0].get("FIRSTNAME"));
        assertNotNull(rows[1]);
        assertEquals("JIMMY", rows[1].get("FIRSTNAME"));
        assertNotNull(rows[2]);
        assertEquals("JANE", rows[2].get("FIRSTNAME"));
        assertNotNull(rows[3]);
        assertEquals("FRED", rows[3].get("FIRSTNAME"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void fixedHeaderAndTrailerShouldSucceed() throws IOException {
        String data = IOUtils.toString(getClass().getResourceAsStream("/PEOPLE-HeaderAndTrailer.txt"),
                StandardCharsets.UTF_8);
        Map<String, String>[] rows = given().body(data).when().get("/flatpack/headerAndTrailer").then().statusCode(200)
                .extract().as(Map[].class);
        assertNotNull(rows);
        assertEquals(6, rows.length);

        // Assert Flatpack Header
        assertNotNull(rows[0]);
        assertEquals("HBT", rows[0].get("INDICATOR"));
        assertEquals("20080817", rows[0].get("DATE"));

        // Assert Flatpack Main Body
        assertNotNull(rows[1]);
        assertEquals("JOHN", rows[1].get("FIRSTNAME"));
        assertNotNull(rows[2]);
        assertEquals("JIMMY", rows[2].get("FIRSTNAME"));
        assertNotNull(rows[3]);
        assertEquals("JANE", rows[3].get("FIRSTNAME"));
        assertNotNull(rows[4]);
        assertEquals("FRED", rows[4].get("FIRSTNAME"));

        // Assert Flatpack Trailer
        assertNotNull(rows[5]);
        assertEquals("FBT", rows[5].get("INDICATOR"));
        assertEquals("SUCCESS", rows[5].get("STATUS"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void noDescriptorShouldSucceed() throws IOException {
        String data = IOUtils.toString(getClass().getResourceAsStream("/foo.csv"), StandardCharsets.UTF_8);
        Map<String, String>[] rows = given().body(data).when().get("/flatpack/noDescriptor").then().statusCode(200).extract()
                .as(Map[].class);
        assertNotNull(rows);
        assertEquals(4, rows.length);

        assertNotNull(rows[0]);
        assertEquals("James", rows[0].get("NAME"));
        assertNotNull(rows[1]);
        assertEquals("Claus", rows[1].get("NAME"));
        assertNotNull(rows[2]);
        assertEquals("Antoine", rows[2].get("NAME"));
        assertNotNull(rows[3]);
        assertEquals("Xavier", rows[3].get("NAME"));
    }

    @Test
    public void invalidShouldFail() throws IOException {
        String data = IOUtils.toString(getClass().getResourceAsStream("/PEOPLE-FixedLength-Invalid.txt"),
                StandardCharsets.UTF_8);
        given().body(data).when().get("/flatpack/invalid").then().statusCode(200)
                .body(containsString("Flatpack has found 4 errors while parsing."))
                .body(containsString("Line:4 Level:2 Desc:LINE TOO LONG. LINE IS 278 LONG. SHOULD BE 277"));
    }

}
