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
package org.apache.camel.quarkus.component.amqp.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

class AmqpProcessor {

    private static final String QPID_JMS_SERRVICE_BASE = "META-INF/services/org/apache/qpid/jms/";
    private static final String FEATURE = "camel-amqp";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void registerServiceProviders(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageResourceBuildItem> nativeImage) {
        String[] servicePaths = new String[] {
                QPID_JMS_SERRVICE_BASE + "provider/amqp",
                QPID_JMS_SERRVICE_BASE + "provider/amqps",
                QPID_JMS_SERRVICE_BASE + "provider/amqpws",
                QPID_JMS_SERRVICE_BASE + "provider/amqpwss",
                QPID_JMS_SERRVICE_BASE + "provider/failover",
                QPID_JMS_SERRVICE_BASE + "provider/redirects/ws",
                QPID_JMS_SERRVICE_BASE + "provider/redirects/wss",
                QPID_JMS_SERRVICE_BASE + "sasl/ANONYMOUS",
                QPID_JMS_SERRVICE_BASE + "sasl/CRAM-MD5",
                QPID_JMS_SERRVICE_BASE + "sasl/EXTERNAL",
                QPID_JMS_SERRVICE_BASE + "sasl/GSSAPI",
                QPID_JMS_SERRVICE_BASE + "sasl/SCRAM-SHA-1",
                QPID_JMS_SERRVICE_BASE + "sasl/SCRAM-SHA-256",
                QPID_JMS_SERRVICE_BASE + "sasl/XOAUTH2",
                QPID_JMS_SERRVICE_BASE + "tracing/noop",
                QPID_JMS_SERRVICE_BASE + "transports/ssl",
                QPID_JMS_SERRVICE_BASE + "transports/tcp",
                QPID_JMS_SERRVICE_BASE + "transports/ws",
                QPID_JMS_SERRVICE_BASE + "transports/wss",
        };

        for (String path : servicePaths) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, getServiceClass(path)));
        }

        nativeImage.produce(new NativeImageResourceBuildItem(servicePaths));
    }

    private String getServiceClass(String servicePath) {
        try {
            InputStream resource = AmqpProcessor.class.getClassLoader().getResourceAsStream(servicePath);
            Properties properties = new Properties();
            properties.load(resource);
            return properties.getProperty("class");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
