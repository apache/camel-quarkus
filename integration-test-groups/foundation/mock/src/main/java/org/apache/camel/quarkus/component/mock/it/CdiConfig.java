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
package org.apache.camel.quarkus.component.mock.it;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.apache.camel.component.mock.MockComponent;
import org.apache.camel.quarkus.core.events.ComponentAddEvent;
import org.apache.camel.spi.CamelEvent.CamelContextStartedEvent;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CdiConfig {
    private static final Logger LOG = Logger.getLogger(CdiConfig.class);

    private final AtomicBoolean contextStarted = new AtomicBoolean(false);

    public void configureMock(@Observes ComponentAddEvent event) {
        if (event.getComponent() instanceof MockComponent) {
            LOG.info("Customizing the MockComponent");
            MockComponent mockComponent = (MockComponent) event.getComponent();
            assert !mockComponent.isLog();
            /* Perform some custom configuration of the component */
            mockComponent.setLog(true);
            /* Make sure that what we say in docs/modules/ROOT/pages/user-guide/configuration.adoc is true */
            assert !contextStarted.get();
        }
    }

    public void contextStarted(@Observes CamelContextStartedEvent event) {
        LOG.info("Camel context started");
        contextStarted.set(true);
    }

}
