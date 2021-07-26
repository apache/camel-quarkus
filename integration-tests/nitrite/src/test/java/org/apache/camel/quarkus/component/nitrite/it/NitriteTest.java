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
package org.apache.camel.quarkus.component.nitrite.it;

import java.util.GregorianCalendar;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.component.nitrite.NitriteConstants;
import org.dizitart.no2.Document;

import static org.hamcrest.core.Is.is;

@QuarkusTest
@QuarkusTestResource(NitriteTestResource.class)
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NitriteTest {

    private static final EmployeeSerializable sheldonSerializable = new EmployeeSerializable(1L,
            new GregorianCalendar(2010, 10, 1).getTime(),
            "Sheldon",
            "Alpha Centauri");
    private static final EmployeeSerializable leonardSerializable = new EmployeeSerializable(2L,
            new GregorianCalendar(2015, 10, 1).getTime(),
            "Leonard", "Earth");
    private static final EmployeeSerializable irmaSerializable = new EmployeeSerializable(3L,
            new GregorianCalendar(2011, 10, 1).getTime(),
            "Irma",
            "Jupiter");

    private static final EmployeeMappable sheldonMappable = new EmployeeMappable(1L,
            new GregorianCalendar(2010, 10, 1).getTime(),
            "Sheldon",
            "Alpha Centauri");
    private static final EmployeeMappable leonardMappable = new EmployeeMappable(2L,
            new GregorianCalendar(2015, 10, 1).getTime(),
            "Leonard", "Earth");
    private static final EmployeeMappable irmaMappable = new EmployeeMappable(3L, new GregorianCalendar(2011, 10, 1).getTime(),
            "Irma",
            "Jupiter");

    //@Test
    public void repositoryClassSerializable() throws CloneNotSupportedException {
        testRepositoryClass(sheldonSerializable, leonardSerializable, irmaSerializable);
    }

    //@Test
    public void repositoryClassMappable() throws CloneNotSupportedException {
        testRepositoryClass(sheldonMappable, leonardMappable, irmaMappable);
    }

    private void testRepositoryClass(Employee sheldon, Employee leonard, Employee irma)
            throws CloneNotSupportedException {
        boolean mappable = sheldon instanceof EmployeeMappable;
        /* Make sure there is no event there before we start inserting */
        RestAssured.get("/nitrite/getRepositoryClass?mappable=" + mappable)
                .then()
                .statusCode(204);

        /* Insert Sheldon */
        RestAssured.given()
                .contentType(ContentType.JSON)
                .queryParam("mappable", mappable)
                .body(sheldon)
                .post("/nitrite/repositoryClass")
                .then()
                .statusCode(200)
                .body("name", is("Sheldon"));
        RestAssured.get("/nitrite/getRepositoryClass?mappable=" + mappable)
                .then()
                .statusCode(200)
                .header(NitriteConstants.CHANGE_TYPE, "INSERT")
                .body("name", is("Sheldon"));

        /* Insert Leonard */
        RestAssured.given()
                .contentType(ContentType.JSON)
                .queryParam("mappable", mappable)
                .body(leonard)
                .post("/nitrite/repositoryClass")
                .then()
                .statusCode(200)
                .body("name", is("Leonard"));
        RestAssured.get("/nitrite/getRepositoryClass?mappable=" + mappable)
                .then()
                .statusCode(200)
                .header(NitriteConstants.CHANGE_TYPE, "INSERT")
                .body("name", is("Leonard"));

        /* Insert Irma */
        RestAssured.given()
                .contentType(ContentType.JSON)
                .queryParam("mappable", mappable)
                .body(irma)
                .post("/nitrite/repositoryClass")
                .then()
                .statusCode(200)
                .body("name", is("Irma"));
        RestAssured.get("/nitrite/getRepositoryClass?mappable=" + mappable)
                .then()
                .statusCode(200)
                .header(NitriteConstants.CHANGE_TYPE, "INSERT")
                .body("name", is("Irma"));

        Employee updatedSheldon = null;
        if (sheldon instanceof EmployeeSerializable) {
            updatedSheldon = (EmployeeSerializable) sheldon.clone();
        } else {
            updatedSheldon = (EmployeeMappable) sheldon.clone();
        }
        updatedSheldon.setAddress("Moon");

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(new Operation(Operation.Type.update, "name", "Sheldon",
                        mappable ? null : (EmployeeSerializable) updatedSheldon,
                        mappable ? (EmployeeMappable) updatedSheldon : null))
                .queryParam("mappable", mappable)
                .post("/nitrite/repositoryClassOperation")
                .then()
                .body("name", is("Sheldon"),
                        "address", is("Moon"));

        RestAssured.get("/nitrite/getRepositoryClass?mappable=" + mappable)
                .then()
                .statusCode(200)
                .header(NitriteConstants.CHANGE_TYPE, "UPDATE")
                .body("name", is("Sheldon"),
                        "address", is("Moon"));

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(new Operation(Operation.Type.find, "address", (Object) "Moon", null, null))
                .queryParam("mappable", mappable)
                .post("/nitrite/repositoryClassOperation")
                .then()
                .statusCode(200)
                .body("size()", is(1), // After the update, there is 1 employee from the Moon
                        "[0].name", is("Sheldon"));

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(new Operation(Operation.Type.findGt, "empId", (Object) 0, null, null))
                .queryParam("mappable", mappable)
                .post("/nitrite/repositoryClassOperation")
                .then()
                .statusCode(200)
                .body("size()", is(3));// there are 3 employees in total

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(new Operation(Operation.Type.delete, "address", "Moon", null, null))
                .queryParam("mappable", mappable)
                .post("/nitrite/repositoryClassOperation")
                .then()
                .statusCode(204);

        RestAssured
                .get("/nitrite/getRepositoryClass?mappable=" + mappable)
                .then()
                .statusCode(200)
                .header(NitriteConstants.CHANGE_TYPE, "REMOVE")
                .body("name", is("Sheldon"),
                        "address", is("Moon"));

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(new Operation(Operation.Type.findGt, "empId", (Object) 0, null, null))
                .queryParam("mappable", mappable)
                .post("/nitrite/repositoryClassOperation")
                .then()
                .statusCode(200)
                .body("size()", is(2));// there are 2 employees after the deletion

    }

    //@Test
    public void collection() throws Exception {
        /* Make sure there is no event there before we start inserting */
        RestAssured.get("/nitrite/collection")
                .then()
                .statusCode(204);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Document.createDocument("key1", "value1"))
                .post("/nitrite/collection")
                .then()
                .statusCode(200)
                .body("key1", is("value1"));
        RestAssured.get("/nitrite/collection")
                .then()
                .statusCode(200)
                .header(NitriteConstants.CHANGE_TYPE, "INSERT")
                .body("key1", is("value1"));

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Document.createDocument("key2", "value2"))
                .post("/nitrite/collection")
                .then()
                .statusCode(200)
                .body("key2", is("value2"));
        RestAssured.get("/nitrite/collection")
                .then()
                .statusCode(200)
                .header(NitriteConstants.CHANGE_TYPE, "INSERT")
                .body("key2", is("value2"));

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(new Operation(Operation.Type.insert, null, null, Document.createDocument("key1", "value_beforeUpdate")))
                .post("/nitrite/collectionOperation")
                .then()
                .statusCode(200)
                .body("key1", is("value_beforeUpdate"));
        RestAssured.get("/nitrite/collection")
                .then()
                .statusCode(200)
                .header(NitriteConstants.CHANGE_TYPE, "INSERT")
                .body("key1", is("value_beforeUpdate"));

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(new Operation(Operation.Type.update, "key1", "value_beforeUpdate",
                        Document.createDocument("key1", "value_afterUpdate")))
                .post("/nitrite/collectionOperation")
                .then()
                .statusCode(200)
                .body("key1", is("value_afterUpdate"));
        RestAssured.get("/nitrite/collection")
                .then()
                .statusCode(200)
                .header(NitriteConstants.CHANGE_TYPE, "UPDATE")
                .body("key1", is("value_afterUpdate"));

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(new Operation(Operation.Type.delete, "key1", "value1", (Document) null))
                .post("/nitrite/collectionOperation")
                .then()
                .statusCode(204);
        RestAssured.get("/nitrite/collection")
                .then()
                .statusCode(200)
                .header(NitriteConstants.CHANGE_TYPE, "REMOVE")
                .body("key1", is("value1"));

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(new Operation(Operation.Type.find, "key1", (Object) "value_afterUpdate", null))
                .post("/nitrite/collectionOperation")
                .then()
                .statusCode(200)
                .body("size()", is(1));// There is only 1 item with value1
    }

}
