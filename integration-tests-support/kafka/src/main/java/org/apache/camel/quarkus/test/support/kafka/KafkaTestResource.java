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
package org.apache.camel.quarkus.test.support.kafka;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import com.github.dockerjava.api.exception.NotFoundException;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.strimzi.test.container.StrimziKafkaContainer;
import org.apache.camel.quarkus.test.FipsModeUtil;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerFetchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.TestcontainersConfiguration;

public class KafkaTestResource implements QuarkusTestResourceLifecycleManager {
    protected static final String KAFKA_IMAGE_NAME = ConfigProvider.getConfig().getValue("kafka.container.image", String.class);
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaTestResource.class);

    private StrimziKafkaContainer container;
    private GenericContainer j17container;

    @Override
    public Map<String, String> start() {
        LOGGER.info(TestcontainersConfiguration.getInstance().toString());

        try {
            startContainer(KAFKA_IMAGE_NAME, name -> new StrimziKafkaContainer(name));

            return Collections.singletonMap("camel.component.kafka.brokers", container.getBootstrapServers());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String start(Function<String, StrimziKafkaContainer> containerSupplier) {
        LOGGER.info(TestcontainersConfiguration.getInstance().toString());

        //if FIPS environment is present, custom container using J17 has to used because:
        // Password-based encryption support in FIPs mode was implemented in the Red Hat build of OpenJDK 17 update 4
        if (FipsModeUtil.isFipsMode()) {
            //custom image should be cached for the next usages with following id
            String customImageName = "camel-quarkus-test-custom-" + KAFKA_IMAGE_NAME.replaceAll("[\\./]", "-");

            try {
                //in case that the image is not accessible, fetch exception is thrown
                startContainer(customImageName, containerSupplier);
            } catch (ContainerFetchException e) {
                if (e.getCause() instanceof NotFoundException) {
                    LOGGER.info("Custom image for kafka (%s) does not exist. Has to be created.", customImageName);

                    //start of the customized container will create the image
                    //it is not possible to customize existing StrimziKafkaContainer. Testcontainer API doe not allow
                    //to customize the image.
                    // This workaround can be removed once the strimzi container with openjdk 17 is released.
                    // According to https://strimzi.io/blog/2023/01/25/running-apache-kafka-on-fips-enabled-kubernetes-cluster/
                    // image should exist
                    j17container = new GenericContainer(
                            new ImageFromDockerfile(customImageName, false)
                                    .withDockerfileFromBuilder(builder -> builder
                                            .from("quay.io/strimzi-test-container/test-container:latest-kafka-3.2.1")
                                            .env("JAVA_HOME", "/usr/lib/jvm/jre-17")
                                            .env("PATH", "/usr/lib/jvm/jre-17/bin:$PATH")
                                            .user("root")
                                            .run("microdnf install -y --nodocs java-17-openjdk-headless glibc-langpack-en && microdnf clean all")));
                    j17container.start();

                    LOGGER.info("Custom image for kafka (%s) has been created.", customImageName);

                    //start kafka container again
                    startContainer(customImageName, containerSupplier);
                }
            }
        } else {
            startContainer(KAFKA_IMAGE_NAME, containerSupplier);
        }

        return container.getBootstrapServers();

    }

    private void startContainer(String imageName, Function<String, StrimziKafkaContainer> containerSupplier) {
        container = containerSupplier.apply(imageName);

        /* Added container startup logging because of https://github.com/apache/camel-quarkus/issues/2461 */
        container.withLogConsumer(frame -> System.out.print(frame.getUtf8String()))
                //                .withEnv("KAFKA_LOG4J_OPTS", "-Dlog4j.configuration=file:/log4j.properties")
                .waitForRunning()
                .start();
    }

    @Override
    public void stop() {
        if (container != null) {
            try {
                container.stop();
            } catch (Exception e) {
                // ignored
            }
        }
        if (j17container != null) {
            try {
                j17container.stop();
            } catch (Exception e) {
                // ignored
            }
        }
    }

    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(container,
                new TestInjector.AnnotatedAndMatchesType(InjectKafka.class, StrimziKafkaContainer.class));
    }

}
