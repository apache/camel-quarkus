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
package org.apache.camel.quarkus.component.lumberjack.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Produces;

import io.quarkus.arc.Unremovable;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.lumberjack.LumberjackComponent;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public class LumberjackRoutes extends RouteBuilder {

    @ConfigProperty(name = "camel.lumberjack.ssl.test-port")
    Integer sslPort;
    @ConfigProperty(name = "camel.lumberjack.ssl.none.test-port")
    Integer noSslPort;
    @ConfigProperty(name = "camel.lumberjack.ssl.global.test-port")
    Integer globalSslPort;

    @Inject
    LumberjackSslService sslService;

    /**
     * We need to implement some conditional configuration of the {@link LumberjackComponent} thus we create it
     * programmatically and publish via CDI.
     *
     * @return a configured {@link LumberjackComponent}
     */
    @Produces
    @ApplicationScoped
    @Unremovable
    @Named("lumberjack-global-ssl")
    LumberjackComponent lumberjackGlobalSsl() throws IllegalAccessException, NoSuchFieldException, InstantiationException {
        final LumberjackComponent lumberjackComponent = new LumberjackComponent();
        lumberjackComponent.setCamelContext(getContext());
        lumberjackComponent.setUseGlobalSslContextParameters(true);
        lumberjackComponent.setSslContextParameters(sslService.createServerSSLContextParameters());
        return lumberjackComponent;
    }

    @Produces
    @Named("ssl")
    SSLContextParameters ssl() {
        return sslService.createServerSSLContextParameters();
    }

    @Override
    public void configure() throws Exception {
        // Route without SSL
        from(String.format("lumberjack:0.0.0.0:%s", noSslPort))
                .bean(LumberjackService.class, "addLog");

        // Route with SSL
        from(String.format("lumberjack:0.0.0.0:%s?sslContextParameters=#ssl", sslPort))
                .bean(LumberjackService.class, "addLog");

        // Route with global SSL
        from(String.format("lumberjack-global-ssl:0.0.0.0:%s", globalSslPort))
                .bean(LumberjackService.class, "addLog");
    }
}
