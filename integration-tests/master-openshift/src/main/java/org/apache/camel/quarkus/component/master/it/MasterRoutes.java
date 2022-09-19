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
package org.apache.camel.quarkus.component.master.it;

import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class MasterRoutes extends RouteBuilder {

    @ConfigProperty(name = "application.id")
    String applicationId;

    @Override
    public void configure() throws Exception {
        // Output the id of the application into a file
        from("master:ns:timer:test?period=100")
                .id("leader")
                .setBody(constant("leader"))
                .setHeader(Exchange.FILE_NAME, constant(String.format("%s.txt", applicationId)))
                .log(String.format("Application %s is writing into file", applicationId))
                .to("file:target/cluster/");
    }
}
