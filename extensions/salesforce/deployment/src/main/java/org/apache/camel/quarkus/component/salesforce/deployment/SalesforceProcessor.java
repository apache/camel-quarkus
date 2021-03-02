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
package org.apache.camel.quarkus.component.salesforce.deployment;

import java.util.Arrays;
import java.util.List;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
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

class SalesforceProcessor {
    private static final List<Class<?>> SALESFORCE_REFLECTIVE_CLASSES = Arrays.asList(
            HttpClient.class,
            LoginToken.class,
            LoginError.class,
            NotifyForFieldsEnum.class,
            NotifyForOperationsEnum.class,
            PushTopic.class,
            QueryRecordsPushTopic.class,
            RestChoices.class,
            RestErrors.class,
            ProtocolHandlers.class);

    private static final String FEATURE = "camel-salesforce";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        for (Class<?> type : SALESFORCE_REFLECTIVE_CLASSES) {
            reflectiveClass.produce(
                    new ReflectiveClassBuildItem(true, true, type));
        }
    }
}
