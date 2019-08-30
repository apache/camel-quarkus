package org.apache.camel.quarkus.component.csv.it;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@QuarkusTest
class CsvTest {

    @Test
    public void json2csv() {
        RestAssured.given() //
            .contentType(ContentType.JSON)
            .accept(ContentType.TEXT)
            .body("[{\"name\":\"Melwah\", \"species\":\"Camelus Dromedarius\"},{\"name\":\"Al Hamra\", \"species\":\"Camelus Dromedarius\"}]")
            .post("/csv/json-to-csv")
            .then()
            .statusCode(200)
            .body(is("Melwah,Camelus Dromedarius\r\nAl Hamra,Camelus Dromedarius\r\n"));
    }

    @Test
    public void csv2json() {
        RestAssured.given() //
            .contentType(ContentType.TEXT)
            .accept(ContentType.JSON)
            .body("Melwah,Camelus Dromedarius\r\nAl Hamra,Camelus Dromedarius\r\n")
            .post("/csv/csv-to-json")
            .then()
            .statusCode(200)
            .body(is("[[\"Melwah\",\"Camelus Dromedarius\"],[\"Al Hamra\",\"Camelus Dromedarius\"]]"));
    }

}
