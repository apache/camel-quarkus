package org.acme

import org.apache.camel.quarkus.kotlin.routes
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces

@ApplicationScoped
class Routes {
    @Produces
    fun timerToLogRoute() = routes {
        from("timer:greet?period=5s")
                .setBody().constant("Hello World!")
                .log("\${body}")
    }

    @Produces
    fun greetingRoute() = routes {
        from("direct:greet")
            .setBody().simple("Hello \${body}")
    }
}
