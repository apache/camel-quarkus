package [=javaPackageBase].it;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@QuarkusTest
class [=artifactIdBaseCamelCase]Test {

    @Test
    public void test() {
        final String msg = UUID.randomUUID().toString().replace("-", "");
        RestAssured.given() //
            .contentType(ContentType.TEXT).body(msg).post("/[=artifactIdBase]/post") //
            .then().statusCode(201);

        Assertions.fail("Add some assertions to " + getClass().getName());

        RestAssured.get("/[=artifactIdBase]/get").then().statusCode(200);
    }

}
