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
package org.apache.camel.quarkus.component.timer.it;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.LambdaRouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TimerProducers {

    private static final Logger LOG = Logger.getLogger(TimerProducers.class);

    public static final String LOG_MESSAGE = "Lets's fool io.quarkus.test.common.LauncherUtil.CaptureListeningDataReader: Listening on: http://0.0.0.0:";
    @ConfigProperty(name = "quarkus.http.test-port")
    Optional<Integer> httpTestPort;
    @ConfigProperty(name = "quarkus.http.port")
    Optional<Integer> httpPort;

    private int getEffectivePort() {
        final boolean isNativeMode = "executable".equals(System.getProperty("org.graalvm.nativeimage.kind"));
        Optional<Integer> portSource = isNativeMode ? httpPort : httpTestPort;
        return portSource.isPresent() ? portSource.get().intValue() : 0;
    }

    @jakarta.enterprise.inject.Produces
    public LambdaRouteBuilder lambdaRoute() {
        return rb -> rb.from("timer:bar?repeatCount=1").routeId("bar")
                .process(e -> LOG.info(LOG_MESSAGE + getEffectivePort()));
    }

}
