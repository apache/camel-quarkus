package org.acme

import io.quarkus.test.junit.QuarkusTest
import kotlin.test.assertEquals
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport
import org.junit.jupiter.api.Test

@QuarkusTest
class RoutesTest : CamelQuarkusTestSupport() {
    @Test
    fun `Exchange body is prefixed with a greeting`() {
        val greeting = template.requestBody("direct:greet", "Camel Quarkus Kotlin", String::class.java)
        assertEquals("Hello Camel Quarkus Kotlin", greeting)
    }
}
