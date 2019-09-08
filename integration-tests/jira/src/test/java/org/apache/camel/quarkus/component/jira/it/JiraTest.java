package org.apache.camel.quarkus.component.jira.it;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@QuarkusTest
class JiraTest {

    @Test
    public void test() {
        final String msg = UUID.randomUUID().toString().replace("-", "");
        RestAssured.given() //
            .contentType(ContentType.TEXT).body(msg).post("/jira/post") 
            .then().statusCode(201);

        String body = RestAssured.get("/jira/get").asString();
        Assertions.assertEquals(body, "message");
    }

}
