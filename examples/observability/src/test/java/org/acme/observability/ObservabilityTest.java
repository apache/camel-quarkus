package org.acme.observability;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;

@QuarkusTest
public class ObservabilityTest {

    @Test
    public void metrics() {
        // Verify a expected Camel metric is available
        given()
                .when().accept(ContentType.JSON)
                .get("/metrics/application")
                .then()
                .statusCode(200)
                .body(
                        "'camel.context.status;camelContext=camel-quarkus-observability'", is(1));
    }

    @Test
    public void health() {
        // Verify liveness
        given()
                .when().accept(ContentType.JSON)
                .get("/health/live")
                .then()
                .statusCode(200)
                .body("status", Matchers.is("UP"),
                        "checks.name", containsInAnyOrder("camel-liveness-checks", "camel"),
                        "checks.data.custom-liveness-check", containsInAnyOrder(null, "UP"));

        // Verify readiness
        given()
                .when().accept(ContentType.JSON)
                .get("/health/ready")
                .then()
                .statusCode(200)
                .body("status", Matchers.is("UP"),
                        "checks.name", containsInAnyOrder("camel-readiness-checks", "camel", "Uptime readiness check"),
                        "checks.data.custom-readiness-check", containsInAnyOrder(null, "UP"));
    }
}
