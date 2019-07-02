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
package io.quarkus.camel.component.salesforce.deployment;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.apache.camel.component.salesforce.internal.dto.LoginError;
import org.apache.camel.component.salesforce.internal.dto.LoginToken;
import org.apache.camel.component.salesforce.internal.dto.NotifyForFieldsEnum;
import org.apache.camel.component.salesforce.internal.dto.NotifyForOperationsEnum;
import org.apache.camel.component.salesforce.internal.dto.PushTopic;
import org.apache.camel.component.salesforce.internal.dto.QueryRecordsPushTopic;
import org.apache.camel.component.salesforce.internal.dto.RestChoices;
import org.apache.camel.component.salesforce.internal.dto.RestErrors;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.ProtocolHandlers;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateConfigBuildItem;

class JettyProcessor {
    private static final List<Class<?>> JETTY_REFLECTIVE_CLASSES = Arrays.asList(
            HttpClient.class,
            ProtocolHandlers.class);

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

    @BuildStep
    SubstrateConfigBuildItem process() {
        for (Class<?> i : JETTY_REFLECTIVE_CLASSES) {
            addReflectiveClass(true, false, i.getName());
        }
        addReflectiveClass(true, true, LoginError.class.getName(),
                LoginToken.class.getName(),
                NotifyForFieldsEnum.class.getName(),
                NotifyForOperationsEnum.class.getName(),
                PushTopic.class.getName(),
                QueryRecordsPushTopic.class.getName(),
                RestChoices.class.getName(),
                RestErrors.class.getName());

        return SubstrateConfigBuildItem.builder()
                .addRuntimeInitializedClass("org.eclipse.jetty.io.ByteBufferPool")
                .addRuntimeInitializedClass("org.eclipse.jetty.util.thread.QueuedThreadPool")
                .build();
    }

    private void addReflectiveClass(boolean methods, boolean fields, String... className) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(methods, fields, className));
    }
}
