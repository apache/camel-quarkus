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
package org.apache.camel.quarkus.main;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class CamelRouteProducer {
    /*
     * The BeanInfo#getImplClazz() returns null in case of a produce of primitives ao arrays which
     * cause ContainerBeansBuildItem to fail. This producer method is here only to validate we handle
     * such case.
     */
    @Produces
    public String[] primitiveType() {
        return new String[] {};
    }

    @Produces
    RoutesBuilder producedRoute() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:produced")
                        .id("produced")
                        .to("log:produced");
            }
        };
    }
}
