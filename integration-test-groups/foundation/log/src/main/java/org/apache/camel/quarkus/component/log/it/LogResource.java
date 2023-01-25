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
package org.apache.camel.quarkus.component.log.it;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.main.events.AfterStart;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class LogResource {

    public static final String LOG_MESSAGE = "Lets's fool io.quarkus.test.common.LauncherUtil.CaptureListeningDataReader: Listening on: http://0.0.0.0:";
    @Inject
    ProducerTemplate producerTemplate;

    @ConfigProperty(name = "quarkus.http.test-port")
    Optional<Integer> httpTestPort;
    @ConfigProperty(name = "quarkus.http.port")
    Optional<Integer> httpPort;

    private int getEffectivePort() {
        final boolean isNativeMode = "executable".equals(System.getProperty("org.graalvm.nativeimage.kind"));
        Optional<Integer> portSource = isNativeMode ? httpPort : httpTestPort;
        return portSource.isPresent() ? portSource.get().intValue() : 0;
    }

    public void info(@Observes AfterStart event) {
        producerTemplate.sendBody("log:foo-topic", LOG_MESSAGE + getEffectivePort());
    }

}
