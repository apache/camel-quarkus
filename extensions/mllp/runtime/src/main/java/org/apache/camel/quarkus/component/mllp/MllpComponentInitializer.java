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
package org.apache.camel.quarkus.component.mllp;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.apache.camel.component.mllp.MllpComponent;
import org.apache.camel.quarkus.core.events.ComponentAddEvent;

/**
 * A workaround for https://github.com/apache/camel-quarkus/issues/3442
 * Camel 3.11.x specific
 */
@ApplicationScoped
public class MllpComponentInitializer {
    public void onComponentAdd(@Observes ComponentAddEvent event) {
        if (event.getComponent() instanceof MllpComponent) {
            MllpComponent.setLogPhi(MllpComponent.isLogPhi());
        }
    }
}
