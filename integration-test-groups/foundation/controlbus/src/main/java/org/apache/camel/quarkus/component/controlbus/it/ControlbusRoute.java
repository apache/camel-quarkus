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
package org.apache.camel.quarkus.component.controlbus.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ControlbusRoute extends RouteBuilder {

    private static final Logger LOG = Logger.getLogger(ControlbusResource.class);

    @Inject
    RestartRoutePolicy routePolicy;

    @Override
    public void configure() throws Exception {
        from("direct:control")
                .routeId("control")
                .routePolicy(routePolicy)
                .log("control: ${body}");

        from("direct:status")
                .transform()
                .exchange(e -> e.getContext().getRouteController().getRouteStatus("control").name());

        from("direct:stopRoute")
                .to("controlbus:route?routeId=control&action=stop");

        from("direct:startRoute")
                .to("controlbus:route?routeId=control&action=start");

        from("direct:suspendRoute")
                .to("controlbus:route?routeId=control&action=suspend");

        from("direct:resumeRoute")
                .to("controlbus:route?routeId=control&action=resume");

        from("direct:failRoute")
                .to("controlbus:route?routeId=control&action=fail");

        from("direct:restartRoute")
                .to("controlbus:route?routeId=control&action=restart");
    }
}
