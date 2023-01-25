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
package org.apache.camel.quarkus.component.bean.consume;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.Consume;
import org.apache.camel.Endpoint;
import org.apache.camel.EndpointInject;
import org.apache.camel.component.direct.DirectEndpoint;

/**
 * A bean having a method annotated with {@code @Consume} with a fallback endpoint URI getter whose name ends with
 * {@code Endpoint} and whose return type is {@link Endpoint}.
 */
@ApplicationScoped
public class ConsumeAnnotationWithImplicitEndpointGetterBean {
    @EndpointInject("direct:consumeAnnotationWithImplicitEndpointGetter")
    DirectEndpoint directEndpoint;

    @Consume
    public String specialEvent(String name) {
        return "Consumed " + name + " via direct:consumeAnnotationWithImplicitEndpointGetter";
    }

    public Endpoint getSpecialEventEndpoint() {
        return directEndpoint;
    }

}
