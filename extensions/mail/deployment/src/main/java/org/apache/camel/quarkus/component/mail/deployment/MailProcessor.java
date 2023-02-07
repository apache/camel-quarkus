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
package org.apache.camel.quarkus.component.mail.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;

class MailProcessor {

    private static final String FEATURE = "camel-mail";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void registerOkhttpServiceProvider(BuildProducer<ServiceProviderBuildItem> services) {
        services.produce(
                new ServiceProviderBuildItem("jakarta.mail.util.StreamProvider", "com.sun.mail.util.MailStreamProvider"));
        services.produce(
                new ServiceProviderBuildItem("jakarta.mail.Provider", "com.sun.mail.imap.IMAPProvider"));
        services.produce(
                new ServiceProviderBuildItem("jakarta.mail.Provider", "com.sun.mail.imap.IMAPSSLProvider"));
        services.produce(
                new ServiceProviderBuildItem("jakarta.mail.Provider", "com.sun.mail.smtp.SMTPSSLProvider"));
        services.produce(
                new ServiceProviderBuildItem("jakarta.mail.Provider", "com.sun.mail.smtp.SMTPProvider"));
        services.produce(
                new ServiceProviderBuildItem("jakarta.mail.Provider", "com.sun.mail.pop3.POP3Provider"));
        services.produce(
                new ServiceProviderBuildItem("jakarta.mail.Provider", "com.sun.mail.pop3.POP3SSLProvider"));
        services.produce(
                new ServiceProviderBuildItem("jakarta.activation.spi.MimeTypeRegistryProvider",
                        "com.sun.activation.registries.MimeTypeRegistryProviderImpl"));
        services.produce(
                new ServiceProviderBuildItem("jakarta.activation.spi.MailcapRegistryProvider",
                        "com.sun.activation.registries.MailcapRegistryProviderImpl"));
    }

}
