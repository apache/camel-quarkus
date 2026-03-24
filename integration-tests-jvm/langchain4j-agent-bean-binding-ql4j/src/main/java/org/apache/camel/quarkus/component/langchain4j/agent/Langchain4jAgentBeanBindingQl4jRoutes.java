package org.apache.camel.quarkus.component.langchain4j.agent;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class Langchain4jAgentBeanBindingQl4jRoutes extends RouteBuilder {

    @Override
    public void configure() {

        from("direct:ai-service-should-be-resolvable-by-interface")
                .bean(AiServiceResolvedByInterface.class);

        from("direct:ai-service-should-be-resolvable-by-name")
                .bean("aiServiceResolvedByName");
    }
}
