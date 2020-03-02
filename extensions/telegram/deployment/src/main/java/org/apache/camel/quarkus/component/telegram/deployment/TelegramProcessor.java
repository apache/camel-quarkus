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
package org.apache.camel.quarkus.component.telegram.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.quarkus.component.telegram.TelegramRecorder;
import org.apache.camel.quarkus.core.deployment.CamelBeanBuildItem;

class TelegramProcessor {

    private static final String FEATURE = "camel-telegram";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelBeanBuildItem telegramComponent(TelegramRecorder recorder) {
        return new CamelBeanBuildItem(
                "telegram",
                "org.apache.camel.component.telegram.TelegramComponent",
                recorder.createTelegramComponent());
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    ReflectiveClassBuildItem reflectiveMethodsAndFields() {
        return new ReflectiveClassBuildItem(true, true,
                "org.apache.camel.component.telegram.model.Chat",
                "org.apache.camel.component.telegram.model.EditMessageLiveLocationMessage",
                "org.apache.camel.component.telegram.model.IncomingAudio",
                "org.apache.camel.component.telegram.model.IncomingDocument",
                "org.apache.camel.component.telegram.model.IncomingMessage",
                "org.apache.camel.component.telegram.model.IncomingPhotoSize",
                "org.apache.camel.component.telegram.model.IncomingVideo",
                "org.apache.camel.component.telegram.model.InlineKeyboardButton",
                "org.apache.camel.component.telegram.model.Location",
                "org.apache.camel.component.telegram.model.MessageResult",
                "org.apache.camel.component.telegram.model.OutgoingAudioMessage",
                "org.apache.camel.component.telegram.model.OutgoingDocumentMessage",
                "org.apache.camel.component.telegram.model.OutgoingMessage",
                "org.apache.camel.component.telegram.model.OutgoingPhotoMessage",
                "org.apache.camel.component.telegram.model.OutgoingTextMessage",
                "org.apache.camel.component.telegram.model.OutgoingVideoMessage",
                "org.apache.camel.component.telegram.model.ReplyKeyboardMarkup",
                "org.apache.camel.component.telegram.model.SendLocationMessage",
                "org.apache.camel.component.telegram.model.SendVenueMessage",
                "org.apache.camel.component.telegram.model.StopMessageLiveLocationMessage",
                "org.apache.camel.component.telegram.model.UnixTimestampDeserializer",
                "org.apache.camel.component.telegram.model.Update",
                "org.apache.camel.component.telegram.model.UpdateResult",
                "org.apache.camel.component.telegram.model.User",
                "org.apache.camel.component.telegram.model.WebhookInfo",
                "org.apache.camel.component.telegram.model.WebhookResult");
    }

}
