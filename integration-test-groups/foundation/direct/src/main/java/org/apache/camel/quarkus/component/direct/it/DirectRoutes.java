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
package org.apache.camel.quarkus.component.direct.it;

import org.apache.camel.builder.RouteBuilder;
import org.jboss.logging.Logger;

public class DirectRoutes extends RouteBuilder {
    private static final Logger LOG = Logger.getLogger(DirectRoutes.class);

    @Override
    public void configure() throws Exception {
        routeTemplate("myTemplate").templateParameter("uuid").templateParameter("greeting")
                .from("direct:{{uuid}}")
                .transform().constant("Hello {{greeting}}");

        from("direct:to-logger")
                .process(e -> LOG.infof("direct:log: %s", e.getMessage().getBody(String.class)));

        from("direct:consumer-producer")
                .to("direct:to-logger");

    }
}
