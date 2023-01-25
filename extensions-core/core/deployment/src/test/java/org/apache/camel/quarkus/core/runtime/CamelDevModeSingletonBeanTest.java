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
package org.apache.camel.quarkus.core.runtime;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import io.quarkus.test.QuarkusDevModeTest;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.camel.builder.RouteBuilder;
import org.awaitility.Awaitility;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class CamelDevModeSingletonBeanTest {

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setLogRecordPredicate(record -> record.getLevel().equals(Level.INFO))
            .setArchiveProducer(() -> ShrinkWrap
                    .create(JavaArchive.class)
                    .addClasses(Routes.class, GreetingBean.class)
                    .addAsResource(applicationProperties(), "application.properties"));

    public static final String LOG_MESSAGE = UUID.randomUUID().toString();

    public static Asset applicationProperties() {
        Writer writer = new StringWriter();

        Properties props = new Properties();
        props.setProperty("quarkus.banner.enabled", "false");
        try {
            props.store(writer, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new StringAsset(writer.toString());
    }

    @Test
    public void testDevModeSingletonBeanInvocation() {
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
            return TEST.getLogRecords().stream().anyMatch(logRecord -> logRecord.getMessage().contains(LOG_MESSAGE));
        });
    }

    @Singleton
    @Named("greeting")
    public static class GreetingBean {
        public String greet() {
            return LOG_MESSAGE;
        }
    }

    public static class Routes extends RouteBuilder {
        @Override
        public void configure() {
            from("timer:invokeBean?repeatCount=1")
                    .bean("greeting", "greet")
                    .log("${body}");
        }
    }

}
