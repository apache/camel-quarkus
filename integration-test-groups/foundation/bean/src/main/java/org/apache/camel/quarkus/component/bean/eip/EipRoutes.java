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
package org.apache.camel.quarkus.component.bean.eip;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.camel.Consume;
import org.apache.camel.builder.RouteBuilder;

public class EipRoutes extends RouteBuilder {

    @Override
    public void configure() {
        from("direct:dynamicRouter")
                .dynamicRouter().method("myDynamicRouter", "route");

    }

    @Produces
    @Singleton
    @Named("dynamicRouterResult0")
    List<String> dynamicRouterResult0() {
        return new ArrayList<>();
    }

    @Produces
    @Singleton
    @Named("dynamicRouterResult1")
    List<String> dynamicRouterResult1() {
        return new ArrayList<>();
    }

    @Singleton
    @Named("myDynamicRouter")
    @RegisterForReflection
    static class CustomDynamicRouter {
        public String route(String body) {
            try {
                int val = Integer.parseInt(body);
                return "bean:dynamicRouterResult" + (val % 2) + "?method=add";
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    @RegisterForReflection(targets = ArrayList.class) // for dynamicRouterAnnotationResult
    static class DynamicRouterWithAnnotation {

        @Consume("direct:dynamicRouterAnnotation")
        @org.apache.camel.DynamicRouter
        public String route(String body) {
            try {
                int val = Integer.parseInt(body);
                return "bean:dynamicRouterAnnotationResult" + (val % 2) + "?method=add";
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    @Produces
    @Singleton
    @Named("dynamicRouterAnnotationResult0")
    List<String> dynamicRouterResult2() {
        return new ArrayList<>();
    }

    @Produces
    @Singleton
    @Named("dynamicRouterAnnotationResult1")
    List<String> dynamicRouterResult3() {
        return new ArrayList<>();
    }

}
