package org.apache.camel.quarkus.component.quartz.it;

import org.apache.camel.builder.RouteBuilder;

public class QuartzRoutes extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("quartz:0/1 * * * * ?")
                .setBody(constant("Hello Camel Quarkus Quartz"))
                .to("seda:quartz-result");
    }
}
