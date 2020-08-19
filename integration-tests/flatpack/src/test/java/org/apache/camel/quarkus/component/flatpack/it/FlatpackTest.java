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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

@QuarkusTest
class FlatpackTest {

    @Test
    public void delimitedUnmarshalShouldSucceed() throws IOException {
        String data = IOUtils.toString(getClass().getResourceAsStream("/delim/INVENTORY-CommaDelimitedWithQualifier.txt"),
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

        String expected = "ITEM_DESC,IN_STOCK,PRICE,LAST_RECV_DT\nAN ENGINE,100,1000.00,20040601\n";
        given().contentType(ContentType.JSON).body(data).when().get("/flatpack/delimited-marshal").then().statusCode(200)
                .body(is(expected));
    }

    @Test
    public void fixedLengthUnmarshalShouldSucceed() throws IOException {
        String data = IOUtils.toString(getClass().getResourceAsStream("/fixed/PEOPLE-FixedLength.txt"), StandardCharsets.UTF_8);
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

}
