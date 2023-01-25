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
package org.apache.camel.quarkus.component.bean.method;

import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.camel.builder.RouteBuilder;

/**
 * A {@link RouteBuilder} instantiated by Camel (not by Arc).
 */
@ApplicationScoped
public class BeanMethodRoute extends RouteBuilder {

    @Inject
    @Named("collected-names")
    Map<String, List<String>> collectedNames;

    private final NonRegisteredBean beanInstance = new NonRegisteredBean();

    @Override
    public void configure() {
        from("direct:beanFromRegistryByName")
                .filter().method("RegisteredBean", "isSenior")
                .transform(method("RegisteredBean", "toFirstName"))
                .process(e -> collectedNames.get("beanFromRegistryByName").add(e.getMessage().getBody(String.class)));

        from("direct:beanByClassName")
                .filter().method(NonRegisteredBean.class, "isJunior")
                .transform(method(NonRegisteredBean.class, "toLastName"))
                .process(e -> collectedNames.get("beanByClassName").add(e.getMessage().getBody(String.class)));

        from("direct:beanInstance")
                .filter().method(beanInstance, "isJunior")
                .transform(method(beanInstance, "toLastName"))
                .process(e -> collectedNames.get("beanInstance").add(e.getMessage().getBody(String.class)));

    }

}
