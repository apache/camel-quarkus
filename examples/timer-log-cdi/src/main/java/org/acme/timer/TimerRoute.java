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
package org.acme.timer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * A {@link RouteBuilder} demonstrating the use of CDI (Contexts and Dependency Injection).
 * <p>
 * Note that for the {@code @Inject} and {@code @ConfigProperty} annotations to work, this class has to be annotated
 * with {@code @ApplicationScoped}.
 */
@ApplicationScoped
public class TimerRoute extends RouteBuilder {

    /** {@code timer.period} is defined in {@code src/main/resources/application.properties} */
    @ConfigProperty(name = "timer.period", defaultValue = "1s")
    String period;

    /** An injected bean */
    @Inject
    Counter counter;

    @Override
    public void configure() throws Exception {
        fromF("timer:foo?period=%s", period)
                .setBody(() -> "Incremented the counter: " + counter.increment())
                .to("log:cdi-example?showExchangePattern=false&showBodyType=false");
    }
}
