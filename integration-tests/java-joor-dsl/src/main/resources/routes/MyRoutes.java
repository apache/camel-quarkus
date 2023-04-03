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
import java.util.function.Function;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.camel.builder.RouteBuilder;

@RegisterForReflection(classNames = "org.apache.camel.quarkus.dsl.java.joor.JavaJoorDslBean", ignoreNested = false)
public class MyRoutes extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        from("direct:joorHello")
            .id("my-java-route")
            .setBody(exchange -> "Hello " + exchange.getMessage().getBody()  + " from jOOR!");
        from("direct:joorHi")
            .id("reflection-route")
            .process(exchange -> {
                Class<?> c = Thread.currentThread().getContextClassLoader().loadClass("org.apache.camel.quarkus.dsl.java.joor.JavaJoorDslBean");
                Object hi = c.getMethod("hi", String.class).invoke(null, exchange.getMessage().getBody());
                Class<?> c2 = Thread.currentThread().getContextClassLoader().loadClass("org.apache.camel.quarkus.dsl.java.joor.JavaJoorDslBean$Inner");
                exchange.getMessage().setBody(c2.getMethod("addSource", String.class).invoke(null, hi));
            });
        from("direct:joorEcho")
                .id("inner-classes-route")
                .process(exchange -> {
                    exchange.getMessage().setBody(Inner.format((String) exchange.getMessage().getBody()));
                });
    }

    public static class Inner {

        public static String format(String result) {
            Function<String, String> toUpperCase = String::toUpperCase;
            return String.format("Msg: %s", toUpperCase.apply(result));
        }
    }
}