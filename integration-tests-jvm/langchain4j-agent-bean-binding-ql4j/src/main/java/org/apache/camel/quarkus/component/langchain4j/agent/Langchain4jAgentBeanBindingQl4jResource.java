package org.apache.camel.quarkus.component.langchain4j.agent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;

@Path("/langchain4j-agent-bean-binding-ql4j")
@ApplicationScoped
public class Langchain4jAgentBeanBindingQl4jResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/ai-service-should-be-resolvable-by-interface")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String aiServiceShouldBeResolvableByInterface() {
        return producerTemplate.requestBody("direct:ai-service-should-be-resolvable-by-interface", "dummy-body-by-interface",
                String.class);
    }

    @Path("/ai-service-should-be-resolvable-by-name")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String aiServiceShouldBeResolvableByName() {
        return producerTemplate.requestBody("direct:ai-service-should-be-resolvable-by-name", "dummy-body-by-name",
                String.class);
    }

}
